package com.coraybennett.spillway.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coraybennett.spillway.dto.VideoListResponse;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.UserRepository;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.VideoService;

@RestController
@RequestMapping("/video")
public class VideoController {
    private final String CONTENT_PATH_PREFIX = "content/";
    private final VideoService videoService;

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Autowired
    public VideoController(
        VideoService videoService, 
        VideoRepository videoRepository,
        UserRepository userRepository
    ) {
        this.videoService = videoService;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoMetadata(@PathVariable String id) {
        Optional<Video> video = videoService.getVideoById(id);
        return video.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<ByteArrayResource> getVideoPlaylist(@PathVariable String id) throws IOException {
        Optional<Video> video = videoService.getVideoById(id);
        if (video.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        if (video.get().getConversionStatus() != com.coraybennett.spillway.model.ConversionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        String playlistFile = id + ".m3u8";
        
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.set("Content-Disposition", "inline;filename=" + playlistFile);
        
        try {
            ByteArrayResource playlist = fileToByteArrayResource(
                CONTENT_PATH_PREFIX + String.format("%s/%s", id, playlistFile));
            
            if (playlist == null) {
                return ResponseEntity.notFound().build();
            }
            
            return new ResponseEntity<>(playlist, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/segments/{filename}")
    public ResponseEntity<ByteArrayResource> getVideoSegment(
            @PathVariable String id, 
            @PathVariable String filename) throws IOException {
                
        Optional<Video> video = videoService.getVideoById(id);
        if (video.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "video/mp2t");
        headers.set("Content-Disposition", "inline;filename=" + filename);
        
        try {
            ByteArrayResource tsVideo = fileToByteArrayResource(
                CONTENT_PATH_PREFIX + String.format("%s/%s", id, filename));
            
            if (tsVideo == null) {
                return ResponseEntity.notFound().build();
            }
            
            return new ResponseEntity<>(tsVideo, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/my-videos")
    public ResponseEntity<List<VideoListResponse>> getMyVideos(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        List<Video> myVideos = videoRepository.findByUploadedBy(user);
        List<VideoListResponse> videoResponses = myVideos.stream()
            .map(VideoListResponse::new)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(videoResponses);
    }
    
    ByteArrayResource fileToByteArrayResource(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new ByteArrayResource(bytes);
    }
}