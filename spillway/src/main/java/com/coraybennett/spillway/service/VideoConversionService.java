package com.coraybennett.spillway.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.exception.VideoConversionException;

@Service
public class VideoConversionService {
    private final String outputDirectory = "content/";

    public void convertToHls(MultipartFile videoFile) throws VideoConversionException {
        try {
            // Generate a UUID for unique file names
            String uuid = UUID.randomUUID().toString();
            Path outputPath = Paths.get(outputDirectory, uuid);
            Files.createDirectory(outputPath);

            // Original file path
            Path inputPath = Paths.get(outputPath.toString(), videoFile.getOriginalFilename());
            Files.copy(videoFile.getInputStream(), inputPath, StandardCopyOption.REPLACE_EXISTING);

            // Convert the video to HLS format using ffmpeg
            String ffmpegCommand = String.format("ffmpeg -hide_banner -i %s -c copy -flags +global_header -f segment -segment_time 10 %s/%s_%%d.ts -master_pl_name %s.m3u8",
                    inputPath.toAbsolutePath(), outputPath.toAbsolutePath(), uuid, uuid);

            Process process = Runtime.getRuntime().exec(ffmpegCommand);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new VideoConversionException("Error converting video file", e);
        }
    }
}
