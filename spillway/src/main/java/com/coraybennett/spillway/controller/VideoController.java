package com.coraybennett.spillway.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredVideoResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.dto.VideoListResponse;
import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoService;

/**
 * Fully refactored controller handling video-related operations using meta-annotations for access control.
 */
@RestController
@RequestMapping("/video")
public class VideoController {
    private final VideoService videoService;
    private final UserService userService;
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoAccessService videoAccessService;

    @Autowired
    public VideoController(
        VideoService videoService, 
        UserService userService,
        VideoRepository videoRepository,
        StorageService storageService,
        VideoAccessService videoAccessService
    ) {
        this.videoService = videoService;
        this.userService = userService;
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.videoAccessService = videoAccessService;
    }

    @GetMapping("/{id}")
    @SecuredVideoResource(optionalAuth = true)
    public ResponseEntity<?> getVideoMetadata(
            @PathVariable String id, 
            @CurrentUser(required = false) User user,
            @ResolvedResource Video video) {
        
        return ResponseEntity.ok(video);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getConversionStatus(@PathVariable String id) {
        VideoService.ConversionProgress progress = videoService.getConversionProgress(id);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
    
    @GetMapping("/{id}/playlist")
    @SecuredVideoResource(optionalAuth = true)
    public ResponseEntity<ByteArrayResource> getVideoMasterPlaylist(
            @PathVariable String id,
            @CurrentUser(required = false) User user,
            @ResolvedResource Video video) throws IOException {
        
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/playlist/{quality}")
    @SecuredVideoResource(optionalAuth = true, handling = SecuredVideoResource.ResourceHandling.VERIFY_ONLY)
    public ResponseEntity<ByteArrayResource> getVideoQualityPlaylist(
            @PathVariable String id, 
            @PathVariable String quality,
            @CurrentUser(required = false) User user) throws IOException {
        
        Optional<Video> video = videoService.getVideoById(id);
        
        if (video.get().getConversionStatus() != ConversionStatus.COMPLETED) {
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
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/segments/{filename}")
    @SecuredVideoResource(optionalAuth = true, handling = SecuredVideoResource.ResourceHandling.VERIFY_ONLY)
    public ResponseEntity<ByteArrayResource> getVideoSegment(
            @PathVariable String id, 
            @PathVariable String filename,
            @CurrentUser(required = false) User user) throws IOException {
            
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
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/my-videos")
    @UserAction
    public ResponseEntity<List<VideoListResponse>> getMyVideos(@CurrentUser User user) {
        List<Video> myVideos = videoService.listVideos(user.getId());
        List<VideoListResponse> videoResponses = myVideos.stream()
            .map(VideoListResponse::new)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(videoResponses);
    }

    @PutMapping("/{id}")
    @SecuredVideoResource(requireWrite = true)
    public ResponseEntity<?> updateVideo(
            @PathVariable String id, 
            @RequestBody Video videoDetails, 
            @CurrentUser User user,
            @ResolvedResource Video video) {
        
        video.setTitle(videoDetails.getTitle());
        video.setDescription(videoDetails.getDescription());
        video.setGenre(videoDetails.getGenre());
        
        Video updatedVideo = videoRepository.save(video);
        
        return ResponseEntity.ok(new VideoListResponse(updatedVideo));
    }
}