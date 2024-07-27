package com.coraybennett.spillway.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.exception.VideoConversionException;

@Service
public class VideoConversionService {
    private final String OUTPUT_DIRECTORY = "content/";
    private final String VIDEO_ROUTE_PREFIX = "http://127.0.0.1:8081/video/";

    public String convertToHls(MultipartFile videoFile) throws VideoConversionException {
        try {
            // Generate a UUID for unique file names
            String uuid = UUID.randomUUID().toString();
            Path outputPath = Paths.get(OUTPUT_DIRECTORY, uuid);
            Files.createDirectory(outputPath);

            // Original file path
            Path inputPath = Paths.get(outputPath.toString(), videoFile.getOriginalFilename());
            Files.copy(videoFile.getInputStream(), inputPath, StandardCopyOption.REPLACE_EXISTING);

            String outputPlaylist = outputPath.toAbsolutePath().toString() + "/" + uuid + ".m3u8";

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
                outputPlaylist
            );
            
            Process process = Runtime.getRuntime().exec(ffmpegCommand);
            int exitCode = process.waitFor();

            Files.delete(inputPath);
            
            if (exitCode != 0) {
                Files.delete(outputPath);
                throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
            }

            processPlaylistFile(outputPlaylist, uuid);

            return uuid;
        } catch (IOException | InterruptedException e) {
            throw new VideoConversionException("Error converting video file", e);
        }
    }

    private void processPlaylistFile(String path, String uuid) throws FileNotFoundException, IOException {
        File playlist = new File(path);
            List<String> processedPlaylist;
            try (Scanner scanner = new Scanner(playlist)) {
                processedPlaylist = new ArrayList<>();
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.matches(uuid + "[0-9]+.ts")) {
                        processedPlaylist.add(VIDEO_ROUTE_PREFIX + uuid + "/" + line);
                        continue;
                    }
                    processedPlaylist.add(line);
                }
            }
            
            try (FileWriter writer = new FileWriter(playlist)) {
                for (String line : processedPlaylist) {
                    writer.write(line + "\n");
                }
            }
    }
}
