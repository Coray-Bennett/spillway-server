package com.coraybennett.spillway.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.dto.VideoResponse;
import com.coraybennett.spillway.dto.VideoUploadRequest;
import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoService;

/**
 * Controller handling file upload operations.
 */
@RestController
@RequestMapping("/upload")
public class FileUploadController {
    private final VideoService videoService;
    private final UserService userService;

    @Autowired
    public FileUploadController(VideoService videoService, UserService userService) {
        this.videoService = videoService;
        this.userService = userService;
    }

    @PostMapping("/video/metadata")
    public ResponseEntity<VideoResponse> createVideoMetadata( 
        @RequestBody VideoUploadRequest metadata,
        Principal principal
    ) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            VideoResponse response = videoService.createVideo(metadata, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/video/{videoId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideoFile(
            @PathVariable String videoId,
            @RequestParam("file") MultipartFile videoFile,
            Principal principal
    ) {
        try {
            if(!videoService.getVideoById(videoId).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Video with id " + videoId + " not found.");
            }
            
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            String uploaderId = videoService.getVideoById(videoId).get().getUploadedBy().getId(); 
            if(!uploaderId.equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User ID " + user.getId() + " does not match uploader ID " + uploaderId);
            }

            videoService.uploadAndConvertVideo(videoId, videoFile);
            return ResponseEntity.accepted().build();
        } catch (VideoConversionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error converting video file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }
}