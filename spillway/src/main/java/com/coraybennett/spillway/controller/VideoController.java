package com.coraybennett.spillway.controller;

import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@RestController
@RequestMapping("/video")
public class VideoController {
    private final String CONTENT_PATH_PREFIX = "content/";
    private final VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
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
        
        // Check if conversion is complete
        if (video.get().getConversionStatus() != com.coraybennett.spillway.model.ConversionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build(); // 202 - Processing
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
        headers.set("Content-Type", "video/mp2t"); // Correct content type for .ts files
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
    
    ByteArrayResource fileToByteArrayResource(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new ByteArrayResource(bytes);
    }
}