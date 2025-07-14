package com.coraybennett.spillway.controller;

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
import com.coraybennett.spillway.service.api.VideoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller handling file upload operations.
 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    private final VideoService videoService;

    @PostMapping("/video/metadata")
    @UserAction
    @Loggable(entryMessage = "Create video metadata", includeParameters = true, includeResult = true)
    public ResponseEntity<VideoResponse> createVideoMetadata( 
        @RequestBody VideoUploadRequest metadata,
        @CurrentUser User user
    ) {
        try {
            VideoResponse response = videoService.createVideo(metadata, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
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
            @ResolvedResource Video video
    ) {
        try {
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