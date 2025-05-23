package com.coraybennett.spillway.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.VideoConversionService;

/**
 * FFmpeg-based implementation of VideoConversionService.
 */
@Service
public class FFmpegVideoConversionService implements VideoConversionService {
    private static final Logger logger = LoggerFactory.getLogger(FFmpegVideoConversionService.class);
    
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final Map<String, Process> activeConversions = new ConcurrentHashMap<>();
    
    private final String outputDirectory;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    @Autowired
    public FFmpegVideoConversionService(
            VideoRepository videoRepository, 
            StorageService storageService,
            @Value("${video.output-directory:content}") String outputDirectory) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.outputDirectory = outputDirectory;
    }

    @Override
    @Async("videoConversionExecutor")
    public CompletableFuture<Void> convertToHls(Path sourceFile, Video video) {
        Path outputPath = null;
        
        try {
            video.setConversionStatus(ConversionStatus.IN_PROGRESS);
            videoRepository.save(video);
            
            outputPath = Paths.get(getOutputDirectory().toString(), video.getId());
            Files.createDirectories(outputPath);
            String outputPlaylist = outputPath.toAbsolutePath().toString() + File.separator + video.getId() + ".m3u8";
            List<String> command = buildFfmpegCommand(sourceFile.toAbsolutePath().toString(), outputPlaylist);
            
            logger.info("Starting FFmpeg conversion for video: {}", video.getId());
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            // Keep track of active process for potential cancellation
            activeConversions.put(video.getId(), process);
            
            // Parse FFmpeg output for progress
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            parseFFmpegOutput(errorReader, video);
            
            int exitCode = process.waitFor();
            activeConversions.remove(video.getId());
            
            // Delete input file after processing
            storageService.delete(sourceFile);
            
            if (exitCode != 0) {
                throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
            }
            
            processPlaylistFile(outputPlaylist, video.getId());
            
            video.setConversionStatus(ConversionStatus.COMPLETED);
            video.setConversionProgress(100);
            video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, video.getId()));
            videoRepository.save(video);
            
            logger.info("Completed FFmpeg conversion for video: {}", video.getId());
            
            return CompletableFuture.completedFuture(null);
            
        } catch (VideoConversionException | IOException | InterruptedException e) {
            logger.error("Error during video conversion", e);
            
            activeConversions.remove(video.getId());
            cleanupOnError(sourceFile, outputPath);
            
            video.setConversionStatus(ConversionStatus.FAILED);
            video.setConversionError(e.getMessage());
            videoRepository.save(video);
            
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public boolean cancelConversion(String videoId) {
        Process process = activeConversions.get(videoId);
        if (process != null && process.isAlive()) {
            process.destroy();
            activeConversions.remove(videoId);
            
            // Update video status
            videoRepository.findById(videoId).ifPresent(video -> {
                video.setConversionStatus(ConversionStatus.FAILED);
                video.setConversionError("Conversion cancelled by user");
                videoRepository.save(video);
            });
            
            return true;
        }
        return false;
    }

    @Override
    public Path getOutputDirectory() {
        return Paths.get(outputDirectory);
    }

    @Override
    public boolean cleanupVideoFiles(String videoId) {
        try {
            Path videoPath = Paths.get(getOutputDirectory().toString(), videoId);
            return storageService.delete(videoPath);
        } catch (Exception e) {
            logger.error("Failed to cleanup video files: " + videoId, e);
            return false;
        }
    }

    @Override
    public int getVideoDuration(Path videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffprobe");
            command.add("-v");
            command.add("error");
            command.add("-show_entries");
            command.add("format=duration");
            command.add("-of");
            command.add("default=noprint_wrappers=1:nokey=1");
            command.add(videoPath.toString());
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String durationStr = reader.readLine();
            
            int exitCode = process.waitFor();
            if (exitCode != 0 || durationStr == null) {
                logger.warn("Failed to get duration, process exited with code: {}", exitCode);
                return 0;
            }
            
            // Parse the duration (it's in seconds with decimal places)
            double durationDouble = Double.parseDouble(durationStr);
            return (int) Math.round(durationDouble);
            
        } catch (Exception e) {
            logger.warn("Error determining video duration: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Builds the FFmpeg command for HLS conversion.
     */
    protected List<String> buildFfmpegCommand(String inputPath, String outputPlaylist) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputPath);
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
        return command;
    }

    /**
     * Parses the FFmpeg output to track conversion progress.
     */
    private void parseFFmpegOutput(BufferedReader reader, Video video) {
        Pattern durationPattern = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)");
        Pattern progressPattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)");
        
        double totalSeconds = 0;
        String line;
        
        try {
            while ((line = reader.readLine()) != null) {
                Matcher durationMatcher = durationPattern.matcher(line);
                if (durationMatcher.find()) {
                    totalSeconds = parseTimeToSeconds(
                        durationMatcher.group(1),
                        durationMatcher.group(2),
                        durationMatcher.group(3)
                    );
                }
                
                Matcher progressMatcher = progressPattern.matcher(line);
                if (progressMatcher.find() && totalSeconds > 0) {
                    double currentSeconds = parseTimeToSeconds(
                        progressMatcher.group(1),
                        progressMatcher.group(2),
                        progressMatcher.group(3)
                    );
                    
                    int progress = (int) ((currentSeconds / totalSeconds) * 100);
                    video.setConversionProgress(Math.min(progress, 99)); // Cap at 99% until complete
                    videoRepository.save(video);
                }
            }
        } catch (IOException e) {
            logger.warn("Error reading FFmpeg output", e);
        }
    }

    /**
     * Converts time string to seconds.
     */
    private double parseTimeToSeconds(String hours, String minutes, String seconds) {
        return Double.parseDouble(hours) * 3600 +
               Double.parseDouble(minutes) * 60 +
               Double.parseDouble(seconds);
    }

    /**
     * Processes the playlist file to update segment URLs.
     */
    private void processPlaylistFile(String path, String videoId) throws IOException {
        File playlist = new File(path);
        if (!playlist.exists()) {
            throw new IOException("Playlist file not found: " + path);
        }
        
        List<String> processedPlaylist = new ArrayList<>();
        try (Scanner scanner = new Scanner(playlist)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.endsWith(".ts") && !line.startsWith("http")) {
                    processedPlaylist.add(String.format("%s/video/%s/segments/%s", baseUrl, videoId, line));
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
    
    /**
     * Cleans up resources on error.
     */
    private void cleanupOnError(Path inputPath, Path outputPath) {
        if (inputPath != null) {
            storageService.delete(inputPath);
        }
        
        if (outputPath != null) {
            storageService.delete(outputPath);
        }
    }
}