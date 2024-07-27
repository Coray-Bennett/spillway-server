package com.coraybennett.spillway.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.exception.VideoConversionException;

@Service
public class VideoConversionService {
    Logger logger = LoggerFactory.getLogger(VideoConversionService.class);
    private final String outputDirectory = "content/";

    public String convertToHls(MultipartFile videoFile) throws VideoConversionException {
        try {
            // Generate a UUID for unique file names
            String uuid = UUID.randomUUID().toString();
            Path outputPath = Paths.get(outputDirectory, uuid);
            Files.createDirectory(outputPath);

            // Original file path
            Path inputPath = Paths.get(outputPath.toString(), videoFile.getOriginalFilename());
            Files.copy(videoFile.getInputStream(), inputPath, StandardCopyOption.REPLACE_EXISTING);

            // Convert the video to HLS format using ffmpeg
            String ffmpegCommand = String.format(
                """
                ffmpeg -i %s 
                -codec: copy 
                -start_number 0 
                -hls_time 10 
                -hls_list_size 0 
                -hls_playlist_type vod 
                -f hls %s
                """,
                inputPath.toAbsolutePath().toString(), 
                outputPath.toAbsolutePath().toString() + "/" + uuid + ".m3u8"
            );
            
            Process process = Runtime.getRuntime().exec(ffmpegCommand);
            int exitCode = process.waitFor();

            Files.delete(inputPath);
            
            if (exitCode != 0) {
                Files.delete(outputPath);
                throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
            }

            return uuid;
        } catch (IOException | InterruptedException e) {
            throw new VideoConversionException("Error converting video file", e);
        }
    }
}
