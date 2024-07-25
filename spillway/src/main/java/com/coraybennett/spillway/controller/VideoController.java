package com.coraybennett.spillway.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video")
public class VideoController {
    final private String CONTENT_PATH_PREFIX = "src/main/java/com/coraybennett/spillway/content/";

    @RequestMapping("/{tag}.m3u8")
    public ResponseEntity<ByteArrayResource> getVideo(@PathVariable String tag) throws IOException {
        String playlistFile = tag + ".m3u8";

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.set("Content-Disposition", "attachment;filename=" + playlistFile);

        try {
            File file = new File(CONTENT_PATH_PREFIX + String.format("%s/%s", tag, playlistFile));
            if(!file.exists()) {
                return new ResponseEntity<>(new ByteArrayResource(new byte[]{}), HttpStatus.NOT_FOUND);
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            ByteArrayResource resource = new ByteArrayResource(bytes);

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(new ByteArrayResource(new byte[]{}), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
