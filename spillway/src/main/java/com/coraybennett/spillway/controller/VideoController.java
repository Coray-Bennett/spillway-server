package com.coraybennett.spillway.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredVideoResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.annotation.Loggable.LogLevel;
import com.coraybennett.spillway.annotation.SecuredVideoResource.ResourceHandling;
import com.coraybennett.spillway.dto.VideoListResponse;
import com.coraybennett.spillway.dto.VideoMetadataResponse;
import com.coraybennett.spillway.dto.VideoUpdateRequest;
import com.coraybennett.spillway.dto.VideoUpdateResponse;
import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoEncryptionService;
import com.coraybennett.spillway.service.api.VideoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller handling video-related operations with encryption support.
 */
@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {
    private final VideoService videoService;
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoAccessService videoAccessService;
    private final VideoEncryptionService videoEncryptionService;

    /**
     * Get video metadata including ownership and encryption information.
     */
    @GetMapping("/{id}")
    @SecuredVideoResource
    @Loggable(entryMessage = "Get video metadata", includeParameters = true)
    @UserAction
    public ResponseEntity<VideoMetadataResponse> getVideoMetadata(
        @PathVariable("id") String id, 
        @ResolvedResource Video video,
        @CurrentUser User user
    ) {
        if (!videoAccessService.canAccessVideo(video, user)) {
            log.warn("Access denied for video {} to user {}", 
                     id, user != null ? user.getUsername() : "anonymous");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Load the video with required relationships (uploadedBy and playlist)
        Video fullVideo = videoRepository.findWithRelationshipsById(id)
            .orElseThrow(() -> new IllegalArgumentException("Video not found: " + id));
        
        VideoMetadataResponse response = new VideoMetadataResponse(fullVideo);
        return ResponseEntity.ok(response);
    }

    /**
     * Get video conversion status.
     */
    @GetMapping("/{id}/status")
    @Loggable(entryMessage = "Get conversion status", includeParameters = true, includeResult = true)
    public ResponseEntity<VideoService.ConversionProgress> getConversionStatus(
            @PathVariable @org.hibernate.validator.constraints.UUID String id) {
        VideoService.ConversionProgress progress = videoService.getConversionProgress(id);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
    
    /**
     * Get video master playlist for HLS streaming.
     * Encrypted videos require the decryption key in the header.
     */
    @GetMapping("/{id}/playlist")
    @Loggable(entryMessage = "Get video master playlist", includeParameters = true)
    @UserAction
    @SecuredVideoResource
    public ResponseEntity<ByteArrayResource> getVideoMasterPlaylist(
        @PathVariable("id") String id,
        @ResolvedResource Video video,
        @CurrentUser User user,
        @RequestHeader(value = "X-Decryption-Key", required = false) String decryptionKey
    ) throws IOException {
        if (!videoAccessService.canAccessVideo(video, user)) {
            log.warn("Access denied for playlist {} to user {}", 
                     id, user != null ? user.getUsername() : "anonymous");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (video.getConversionStatus() != ConversionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        
        // Check if video is encrypted and validate decryption key
        if (video.isEncrypted()) {
            if (decryptionKey == null || !validateDecryptionKey(decryptionKey, video)) {
                log.warn("Invalid or missing decryption key for encrypted video: {}", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("X-Encryption-Required", "true")
                    .build();
            }
        }

        String playlistFile = id + ".m3u8";
        Path playlistPath = Paths.get(videoService.getVideoConversionService().getOutputDirectory().toString(), 
                                    id, 
                                    playlistFile);
        
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.set("Content-Disposition", "inline;filename=" + playlistFile);
        
        // Add header to indicate if video is encrypted
        if (video.isEncrypted()) {
            headers.set("X-Encrypted-Content", "true");
        }
        
        try {
            if (!storageService.exists(playlistPath)) {
                return ResponseEntity.notFound().build();
            }
            
            ByteArrayResource resource = new ByteArrayResource(
                Files.readAllBytes(playlistPath));
            
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error reading playlist file for video {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get video quality-specific playlist.
     */
    @GetMapping("/{id}/playlist/{quality}")
    @Loggable(entryMessage = "Get video playlist of specific quality", includeParameters = true)
    @SecuredVideoResource
    public ResponseEntity<ByteArrayResource> getVideoQualityPlaylist(
            @PathVariable("id") String id,
            @ResolvedResource Video video,
            @PathVariable String quality,
            @RequestHeader(value = "X-Decryption-Key", required = false) String decryptionKey
    ) throws IOException {
        if (video.getConversionStatus() != ConversionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        
        // Check if video is encrypted
        if (video.isEncrypted()) {
            if (decryptionKey == null || !validateDecryptionKey(decryptionKey, video)) {
                log.warn("Invalid or missing decryption key for encrypted video: {}", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("X-Encryption-Required", "true")
                    .build();
            }
        }

        // Validate quality parameter to prevent directory traversal
        if (!quality.matches("^[a-zA-Z0-9]+p$")) {
            return ResponseEntity.badRequest().build();
        }
        
        String playlistFile = quality + ".m3u8";
        Path playlistPath = Paths.get(videoService.getVideoConversionService().getOutputDirectory().toString(), 
                                    id, 
                                    playlistFile);
        
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.set("Content-Disposition", "inline;filename=" + playlistFile);
        
        if (video.isEncrypted()) {
            headers.set("X-Encrypted-Content", "true");
        }
        
        try {
            if (!storageService.exists(playlistPath)) {
                return ResponseEntity.notFound().build();
            }
            
            ByteArrayResource resource = new ByteArrayResource(
                Files.readAllBytes(playlistPath));
            
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error reading quality playlist for video {} quality {}: {}", 
                      id, quality, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get video segment for HLS streaming.
     * Encrypted segments are decrypted on-the-fly if the correct key is provided.
     */
    @GetMapping("/{id}/segments/{filename}")
    @SecuredVideoResource(handling = ResourceHandling.VERIFY_ONLY)
    public ResponseEntity<ByteArrayResource> getVideoSegment(
            @PathVariable("id") String id,
            @PathVariable String filename,
            @RequestHeader(value = "X-Decryption-Key", required = false) String decryptionKey
    ) throws IOException {
        // Get video to check if it's encrypted
        Video video = videoRepository.findById(id)
            .orElse(null);
        
        if (video == null) {
            return ResponseEntity.notFound().build();
        }
        
        Path segmentPath = Paths.get(videoService.getVideoConversionService().getOutputDirectory().toString(),
                                   id,
                                   filename);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "video/mp2t");
        headers.set("Content-Disposition", "inline;filename=" + filename);
        
        try {
            if (!storageService.exists(segmentPath)) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] segmentData;
            
            if (video.isEncrypted()) {
                // Validate decryption key
                if (decryptionKey == null || !validateDecryptionKey(decryptionKey, video)) {
                    log.warn("Invalid or missing decryption key for encrypted segment: {}/{}", id, filename);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header("X-Encryption-Required", "true")
                        .build();
                }
                
                // Decrypt the segment
                try {
                    segmentData = videoEncryptionService.decryptFile(segmentPath, decryptionKey);
                    headers.set("X-Decrypted-Content", "true");
                } catch (Exception e) {
                    log.error("Failed to decrypt segment {}/{}: {}", id, filename, e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                // Read unencrypted segment
                segmentData = Files.readAllBytes(segmentPath);
            }
            
            ByteArrayResource resource = new ByteArrayResource(segmentData);
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            
        } catch (IOException e) {
            log.error("Error reading segment for video {} segment {}: {}", 
                      id, filename, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get list of videos uploaded by the current user.
     */
    @GetMapping("/my-videos")
    @Loggable(entryMessage = "Get user videos", includeParameters = true)
    @UserAction
    public ResponseEntity<List<VideoListResponse>> getMyVideos(@CurrentUser User user) {
        List<Video> myVideos = videoService.listVideos(user.getId());
        List<VideoListResponse> videoResponses = myVideos.stream()
            .map(VideoListResponse::new)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(videoResponses);
    }

    /**
     * Update video metadata.
     */
    @PutMapping("/{id}")
    @Loggable(level = LogLevel.INFO, entryMessage = "Update video", includeResult = true)
    @SecuredVideoResource(requireWrite = true)
    @UserAction
    public ResponseEntity<VideoUpdateResponse> updateVideo(
        @PathVariable("id") String id, 
        @ResolvedResource Video video,
        @Valid @RequestBody VideoUpdateRequest updateRequest, 
        @CurrentUser User user
    ) {
        if (!user.getId().equals(video.getUploadedBy().getId())) {
            log.warn("User {} attempted to update video {} they don't own", 
                     user.getUsername(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Update only the allowed fields
        video.setTitle(updateRequest.getTitle());
        video.setDescription(updateRequest.getDescription());
        video.setGenre(updateRequest.getGenre());
        
        // Update episode information if provided
        if (updateRequest.getSeasonNumber() != null) {
            video.setSeasonNumber(updateRequest.getSeasonNumber());
        }
        if (updateRequest.getEpisodeNumber() != null) {
            video.setEpisodeNumber(updateRequest.getEpisodeNumber());
        }
        
        Video updatedVideo = videoRepository.save(video);
        
        VideoUpdateResponse response = new VideoUpdateResponse(updatedVideo);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate decryption key for an encrypted video.
     */
    @PostMapping("/{id}/validate-key")
    @Loggable(entryMessage = "Validate decryption key", includeParameters = true)
    @UserAction
    public ResponseEntity<Boolean> validateDecryptionKey(
        @PathVariable("id") String id,
        @RequestBody String decryptionKey,
        @CurrentUser User user
    ) {
        Video video = videoRepository.findById(id).orElse(null);
        
        if (video == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!videoAccessService.canAccessVideo(video, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (!video.isEncrypted()) {
            return ResponseEntity.ok(true);
        }
        
        boolean isValid = validateDecryptionKey(decryptionKey, video);
        return ResponseEntity.ok(isValid);
    }
    
    /**
     * Validates a decryption key against the stored hash.
     */
    private boolean validateDecryptionKey(String decryptionKey, Video video) {
        if (!video.isEncrypted() || video.getEncryptionKeyHash() == null) {
            return true;
        }
        
        if (decryptionKey == null || !videoEncryptionService.isValidKey(decryptionKey)) {
            return false;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(decryptionKey.getBytes(StandardCharsets.UTF_8));
            String keyHash = Base64.getEncoder().encodeToString(hash);
            return keyHash.equals(video.getEncryptionKeyHash());
        } catch (Exception e) {
            log.error("Failed to validate decryption key", e);
            return false;
        }
    }
}