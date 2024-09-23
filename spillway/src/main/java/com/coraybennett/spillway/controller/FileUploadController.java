package com.coraybennett.spillway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.service.VideoConversionService;

@RestController
@RequestMapping("/upload")
public class FileUploadController {
    private final VideoConversionService videoConversionService;

    @Autowired
    public FileUploadController(VideoConversionService videoConversionService) {
        this.videoConversionService = videoConversionService;
    }

    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile videoFile) {
        try {
            String uuid = videoConversionService.convertToHls(videoFile);
            return ResponseEntity.ok(uuid);
        } catch (VideoConversionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error converting video file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
        }
    }
}
