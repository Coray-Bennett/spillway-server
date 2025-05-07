package com.coraybennett.spillway.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.exception.VideoConversionException;

@Service
public class VideoConversionService {
    private static final Logger logger = LoggerFactory.getLogger(VideoConversionService.class);
    
    private final String OUTPUT_DIRECTORY = "content/";
    private final String VIDEO_ROUTE_PREFIX = "http://127.0.0.1:8081/video/";

    public String convertToHls(MultipartFile videoFile) throws VideoConversionException {
        Path outputPath = null;
        Path inputPath = null;
        String uuid = UUID.randomUUID().toString();
        
        try {
            // Create output directory
            outputPath = Paths.get(OUTPUT_DIRECTORY, uuid);
            Files.createDirectories(outputPath);
            
            // Save input file
            inputPath = Paths.get(outputPath.toString(), sanitizeFilename(videoFile.getOriginalFilename()));
            Files.copy(videoFile.getInputStream(), inputPath, StandardCopyOption.REPLACE_EXISTING);
            
            String outputPlaylist = outputPath.toAbsolutePath().toString() + File.separator + uuid + ".m3u8";
            
            // Build FFmpeg command properly
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(inputPath.toAbsolutePath().toString());
            command.add("-codec");
            command.add("copy");
            command.add("-start_number");
            command.add("0");
            command.add("-hls_time");
            command.add("10");
            command.add("-hls_list_size");
            command.add("0");
            command.add("-hls_playlist_type");
            command.add("vod");
            command.add("-f");
            command.add("hls");
            command.add(outputPlaylist);
            
            logger.info("Executing FFmpeg command: {}", String.join(" ", command));
            
            // Use ProcessBuilder instead of Runtime.exec
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Capture and log output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                logger.debug("FFmpeg output: {}", output);
            }
            
            // Wait for process to complete with timeout
            boolean completed = process.waitFor(5, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                throw new VideoConversionException("FFmpeg conversion timed out after 5 minutes");
            }
            
            int exitCode = process.exitValue();
            
            // Clean up input file
            Files.deleteIfExists(inputPath);
            
            if (exitCode != 0) {
                deleteDirectory(outputPath);
                throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
            }
            
            // Process the playlist file
            processPlaylistFile(outputPlaylist, uuid);
            
            return uuid;
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error during video conversion", e);
            
            // Clean up resources on error
            if (inputPath != null) {
                try {
                    Files.deleteIfExists(inputPath);
                } catch (IOException ex) {
                    logger.warn("Failed to delete input file", ex);
                }
            }
            
            if (outputPath != null) {
                try {
                    deleteDirectory(outputPath);
                } catch (IOException ex) {
                    logger.warn("Failed to delete output directory", ex);
                }
            }
            
            throw new VideoConversionException("Error converting video file: " + e.getMessage(), e);
        }
    }

    private void processPlaylistFile(String path, String uuid) throws IOException {
        File playlist = new File(path);
        if (!playlist.exists()) {
            throw new IOException("Playlist file not found: " + path);
        }
        
        List<String> processedPlaylist = new ArrayList<>();
        try (Scanner scanner = new Scanner(playlist)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // Correct the regex pattern to match .ts files
                if (line.endsWith(".ts") && !line.startsWith("http")) {
                    processedPlaylist.add(VIDEO_ROUTE_PREFIX + uuid + "/segments/" + line);
                } else {
                    processedPlaylist.add(line);
                }
            }
        }
        
        try (FileWriter writer = new FileWriter(playlist)) {
            for (String line : processedPlaylist) {
                writer.write(line + "\n");
            }
        }
    }
    
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "input.mp4";
        }
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        logger.warn("Failed to delete: " + p, e);
                    }
                });
        }
    }
}