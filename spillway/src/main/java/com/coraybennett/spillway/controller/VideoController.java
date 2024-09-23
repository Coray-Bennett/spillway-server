package com.coraybennett.spillway.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video")
public class VideoController {
    final private String CONTENT_PATH_PREFIX = "content/";

    @GetMapping("/{tag}.m3u8")
    public ResponseEntity<ByteArrayResource> getVideo(@PathVariable String tag) throws IOException {
        String playlistFile = tag + ".m3u8";

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.set("Content-Disposition", "attachment;filename=" + playlistFile);

        try {
            ByteArrayResource playlist = fileToByteArrayResource(CONTENT_PATH_PREFIX + String.format("%s/%s", tag, playlistFile));
            if(playlist == null) {
                return ResponseEntity.notFound().build();
            }

            return new ResponseEntity<>(playlist, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{tag}/{filename}")
    public ResponseEntity<ByteArrayResource> ts(
        @PathVariable String tag, 
        @PathVariable String filename
    ) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.apple.mpegurl");
        headers.set("Content-Disposition", "attachment;filename=" + filename);
        
        try {
            ByteArrayResource tsVideo = fileToByteArrayResource(CONTENT_PATH_PREFIX + String.format("%s/%s", tag, filename));
            if(tsVideo == null) {
                return ResponseEntity.notFound().build();
            }

            return new ResponseEntity<>(tsVideo, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    ByteArrayResource fileToByteArrayResource(String path) throws IOException {
        File file = new File(path);
        if(!file.exists()) {
            return null;
        }

        byte[] bytes = Files.readAllBytes(file.toPath());
        return new ByteArrayResource(bytes);
    }
}
