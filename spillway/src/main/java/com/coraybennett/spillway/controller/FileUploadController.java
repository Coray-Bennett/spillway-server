package com.coraybennett.spillway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredVideoResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.dto.VideoResponse;
import com.coraybennett.spillway.dto.VideoUploadRequest;
import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.VideoEncryptionService;
import com.coraybennett.spillway.service.api.VideoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller handling file upload operations with encryption support.
 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    private final VideoService videoService;
    private final VideoEncryptionService encryptionService;

    @PostMapping("/video/metadata")
    @UserAction
    @Loggable(entryMessage = "Create video metadata", includeParameters = true, includeResult = true)
    public ResponseEntity<VideoResponse> createVideoMetadata( 
        @RequestBody VideoUploadRequest metadata,
        @CurrentUser User user
    ) {
        try {
            // Validate encryption key if encryption is requested
            if (metadata.isEncrypted()) {
                if (metadata.getEncryptionKey() == null || 
                    !encryptionService.isValidKey(metadata.getEncryptionKey())) {
                    log.warn("Invalid or missing encryption key for encrypted video upload");
                    return ResponseEntity.badRequest()
                        .body(null);
                }
                log.info("Creating encrypted video metadata for user: {}", user.getUsername());
            }
            
            VideoResponse response = videoService.createVideo(metadata, user);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid video metadata: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Failed to create video metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/video/{videoId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecuredVideoResource(requireWrite = true, idParameter = "videoId")
    @Loggable(entryMessage = "Upload video file", includeResult = true)
    public ResponseEntity<?> uploadVideoFile(
            @PathVariable("videoId") String videoId,
            @RequestParam("file") MultipartFile videoFile,
            @CurrentUser User user,
            @ResolvedResource Video video,
            @RequestHeader(value = "X-Encryption-Key", required = false) String encryptionKey
    ) {
        try {
            if (video.isEncrypted() && encryptionKey == null) {
                    log.error("Encryption key not found for encrypted video: {}", videoId);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Encryption key required for encrypted video");
                }
            
            videoService.uploadAndConvertVideo(videoId, videoFile, encryptionKey);
            
            return ResponseEntity.accepted()
                .header("X-Video-Encrypted", String.valueOf(video.isEncrypted()))
                .build();
        } catch (VideoConversionException e) {
            log.error("Video conversion error for {}: {}", videoId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error converting video file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Internal error uploading video {}: {}", videoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }
    
    /**
     * Generate a new encryption key for video upload.
     * This endpoint helps clients generate secure encryption keys.
     */
    @PostMapping("/video/generate-encryption-key")
    @UserAction
    @Loggable(entryMessage = "Generate encryption key")
    public ResponseEntity<String> generateEncryptionKey(@CurrentUser User user) {
        try {
            String key = encryptionService.generateEncryptionKey();
            log.info("Generated encryption key for user: {}", user.getUsername());
            return ResponseEntity.ok(key);
        } catch (Exception e) {
            log.error("Failed to generate encryption key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}