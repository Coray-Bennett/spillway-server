package com.coraybennett.spillway.controller;

import com.coraybennett.spillway.dto.VideoUploadRequest;
import com.coraybennett.spillway.dto.VideoResponse;
import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class FileUploadController {
    private final VideoService videoService;

    @Autowired
    public FileUploadController(VideoService videoService) {
        this.videoService = videoService;
    }

    // Step 1: Create video metadata and get UUID immediately
    @PostMapping("/video/metadata")
    public ResponseEntity<VideoResponse> createVideoMetadata(@RequestBody VideoUploadRequest metadata) {
        try {
            VideoResponse response = videoService.createVideo(metadata);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Step 2: Upload the actual video file
    @PostMapping(value = "/video/{videoId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideoFile(
            @PathVariable String videoId,
            @RequestParam("file") MultipartFile videoFile) {
        
        try {
            videoService.uploadAndConvertVideo(videoId, videoFile);
            return ResponseEntity.accepted().build(); // 202 - Processing started
        } catch (VideoConversionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error converting video file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }
}