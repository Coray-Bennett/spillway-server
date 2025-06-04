package com.coraybennett.spillway.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredVideoResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.annotation.Loggable.LogLevel;
import com.coraybennett.spillway.annotation.SecuredVideoResource.ResourceHandling;
import com.coraybennett.spillway.dto.VideoListResponse;
import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller handling video-related operations.
 * Updated with enhanced sharing support and standardized logging.
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

    @GetMapping("/{id}")
    @SecuredVideoResource
    @Loggable(entryMessage = "Get video metadata", includeParameters = true)
    @UserAction
    public ResponseEntity<?> getVideoMetadata(
        @PathVariable("id") String id, 
        @ResolvedResource Video video,
        @CurrentUser User user
    ) {
        if (!videoAccessService.canAccessVideo(video, user)) {
            log.warn("Access denied for video {} to user {}", 
                     id, user != null ? user.getUsername() : "anonymous");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(video);
    }

    @GetMapping("/{id}/status")
    @Loggable(entryMessage = "Get conversion status", includeParameters = true, includeResult = true)
    public ResponseEntity<?> getConversionStatus(@PathVariable @org.hibernate.validator.constraints.UUID String id) {
        VideoService.ConversionProgress progress = videoService.getConversionProgress(id);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
    
    @GetMapping("/{id}/playlist")
    @Loggable(entryMessage = "Get video master playlist", includeParameters = true)
    @UserAction
    @SecuredVideoResource
    public ResponseEntity<ByteArrayResource> getVideoMasterPlaylist(
        @PathVariable("id") String id,
        @ResolvedResource Video video,
        @CurrentUser User user
    ) throws IOException {
        if (!videoAccessService.canAccessVideo(video, user)) {
            log.warn("Access denied for playlist {} to user {}", 
                     id, user != null ? user.getUsername() : "anonymous");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (video.getConversionStatus() != ConversionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        String playlistFile = id + ".m3u8";
        Path playlistPath = Paths.get(videoService.getVideoConversionService().getOutputDirectory().toString(), 
                                    id, 
                                    playlistFile);
        
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.set("Content-Disposition", "inline;filename=" + playlistFile);
        
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

    @GetMapping("/{id}/playlist/{quality}")
    @Loggable(entryMessage = "Get video playlist of specific quality", includeParameters = true)
    @SecuredVideoResource
    public ResponseEntity<ByteArrayResource> getVideoQualityPlaylist(
            @PathVariable("id") String id,
            @ResolvedResource Video video,
            @PathVariable String quality
    ) throws IOException {
        if (video.getConversionStatus() != com.coraybennett.spillway.model.ConversionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
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

    @GetMapping("/{id}/segments/{filename}")
    @SecuredVideoResource(handling = ResourceHandling.VERIFY_ONLY)
    public ResponseEntity<ByteArrayResource> getVideoSegment(
            @PathVariable("id") String id,
            @PathVariable String filename
    ) throws IOException {
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
            
            ByteArrayResource resource = new ByteArrayResource(
                Files.readAllBytes(segmentPath));
            
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error reading segment for video {} segment {}: {}", 
                      id, filename, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

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

    @PutMapping("/{id}")
    @Loggable(level = LogLevel.INFO, entryMessage = "Update video", includeResult = true)
    @SecuredVideoResource(requireWrite = true)
    @UserAction
    public ResponseEntity<?> updateVideo(
        @PathVariable("id") String id, 
        @ResolvedResource Video video,
        @RequestBody Video videoDetails, 
        @CurrentUser User user
    ) {
        if (!user.getId().equals(video.getUploadedBy().getId())) {
            log.warn("User {} attempted to update video {} they don't own", 
                     user.getUsername(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        video.setTitle(videoDetails.getTitle());
        video.setDescription(videoDetails.getDescription());
        video.setGenre(videoDetails.getGenre());
        
        Video updatedVideo = videoRepository.save(video);
        
        return ResponseEntity.ok(new VideoListResponse(updatedVideo));
    }
}