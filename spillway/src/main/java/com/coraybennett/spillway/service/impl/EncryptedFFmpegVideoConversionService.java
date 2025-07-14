package com.coraybennett.spillway.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
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
import com.coraybennett.spillway.service.api.VideoEncryptionService;

/**
 * Enhanced FFmpeg-based implementation with encryption support.
 */
@Service
// @Primary
public class EncryptedFFmpegVideoConversionService implements EncryptedVideoConversionService {
    private static final Logger logger = LoggerFactory.getLogger(EncryptedFFmpegVideoConversionService.class);
    
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoEncryptionService encryptionService;
    private final Map<String, Process> activeConversions = new ConcurrentHashMap<>();
    
    private final String outputDirectory;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    @Autowired
    public EncryptedFFmpegVideoConversionService(
            VideoRepository videoRepository, 
            StorageService storageService,
            VideoEncryptionService encryptionService,
            @Value("${video.output-directory:content}") String outputDirectory) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.outputDirectory = outputDirectory;
    }

    @Override
    @Async("videoConversionExecutor")
    public CompletableFuture<Void> convertToHls(Path sourceFile, Video video) {
        // Call the overloaded method with null encryption key
        return convertToHls(sourceFile, video, null);
    }

    @Override
    @Async("videoConversionExecutor")
    public CompletableFuture<Void> convertToHls(Path sourceFile, Video video, String encryptionKey) {
        Path outputPath = null;
        
        try {
            video.setConversionStatus(ConversionStatus.IN_PROGRESS);
            videoRepository.save(video);
            
            outputPath = Paths.get(getOutputDirectory().toString(), video.getId());
            Files.createDirectories(outputPath);
            
            if (video.isEncrypted() && encryptionKey != null) {
                // Convert with encryption
                convertWithEncryption(sourceFile, outputPath, video, encryptionKey);
            } else {
                // Standard conversion without encryption
                convertWithoutEncryption(sourceFile, outputPath, video);
            }
            
            // Delete input file after processing
            storageService.delete(sourceFile);
            
            video.setConversionStatus(ConversionStatus.COMPLETED);
            video.setConversionProgress(100);
            video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, video.getId()));
            videoRepository.save(video);
            
            logger.info("Successfully converted video: {} (encrypted: {})", video.getId(), video.isEncrypted());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Error converting video: " + video.getId(), e);
            video.setConversionStatus(ConversionStatus.FAILED);
            video.setConversionError(e.getMessage());
            videoRepository.save(video);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    private void convertWithoutEncryption(Path sourceFile, Path outputPath, Video video) throws Exception {
        String outputPlaylist = outputPath.toAbsolutePath().toString() + File.separator + video.getId() + ".m3u8";
        List<String> command = buildFfmpegCommand(sourceFile.toAbsolutePath().toString(), outputPlaylist);
        
        logger.info("Starting standard FFmpeg conversion for video: {}", video.getId());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        
        activeConversions.put(video.getId(), process);
        
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        parseFFmpegOutput(errorReader, video);
        
        int exitCode = process.waitFor();
        activeConversions.remove(video.getId());
        
        if (exitCode != 0) {
            throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
        }
        
        processPlaylistFile(outputPlaylist, video.getId());
    }
    
    private void convertWithEncryption(Path sourceFile, Path outputPath, Video video, String encryptionKey) throws Exception {
        // First, convert to HLS as normal in a temporary directory
        Path tempOutputPath = Paths.get(outputPath.toString() + "_temp");
        Files.createDirectories(tempOutputPath);
        
        try {
            String tempPlaylist = tempOutputPath.toAbsolutePath().toString() + File.separator + video.getId() + ".m3u8";
            List<String> command = buildFfmpegCommand(sourceFile.toAbsolutePath().toString(), tempPlaylist);
            
            logger.info("Starting FFmpeg conversion with encryption for video: {}", video.getId());
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            activeConversions.put(video.getId(), process);
            
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            parseFFmpegOutput(errorReader, video);
            
            int exitCode = process.waitFor();
            activeConversions.remove(video.getId());
            
            if (exitCode != 0) {
                throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
            }
            
            // Now encrypt all the segments
            encryptSegments(tempOutputPath, outputPath, video.getId(), encryptionKey);
            
            // Process playlist file
            String finalPlaylist = outputPath.toAbsolutePath().toString() + File.separator + video.getId() + ".m3u8";
            processPlaylistFile(finalPlaylist, video.getId());
            
        } finally {
            // Clean up temporary directory
            deleteDirectory(tempOutputPath);
        }
    }
    
    private void encryptSegments(Path tempPath, Path outputPath, String videoId, String encryptionKey) throws Exception {
        logger.info("Encrypting segments for video: {}", videoId);
        
        // Copy and encrypt playlist file
        Path tempPlaylist = tempPath.resolve(videoId + ".m3u8");
        Path outputPlaylist = outputPath.resolve(videoId + ".m3u8");
        Files.copy(tempPlaylist, outputPlaylist);
        
        // Find and encrypt all segment files (.ts files)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempPath, "*.ts")) {
            for (Path segmentFile : stream) {
                String fileName = segmentFile.getFileName().toString();
                Path outputSegment = outputPath.resolve(fileName);
                
                // Encrypt the segment
                encryptionService.encryptFile(segmentFile, outputSegment, encryptionKey);
                logger.debug("Encrypted segment: {}", fileName);
            }
        }
        
        // Also handle quality-specific playlists if they exist
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempPath, "*p.m3u8")) {
            for (Path qualityPlaylist : stream) {
                String fileName = qualityPlaylist.getFileName().toString();
                Path outputQualityPlaylist = outputPath.resolve(fileName);
                Files.copy(qualityPlaylist, outputQualityPlaylist);
            }
        }
    }
    
    private void deleteDirectory(Path path) {
        try {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        logger.warn("Failed to delete: {}", p);
                    }
                });
        } catch (IOException e) {
            logger.warn("Failed to delete directory: {}", path);
        }
    }

    @Override
    public boolean cancelConversion(String videoId) {
        Process process = activeConversions.get(videoId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            activeConversions.remove(videoId);
            logger.info("Cancelled conversion for video: {}", videoId);
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
            Path videoDir = Paths.get(outputDirectory, videoId);
            if (Files.exists(videoDir)) {
                deleteDirectory(videoDir);
                logger.info("Cleaned up files for video: {}", videoId);
                return true;
            }
            return false;
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
            
            double durationDouble = Double.parseDouble(durationStr);
            return (int) Math.round(durationDouble);
            
        } catch (Exception e) {
            logger.warn("Error determining video duration: {}", e.getMessage());
            return 0;
        }
    }
    
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
                    video.setConversionProgress(Math.min(progress, 99));
                    videoRepository.save(video);
                }
            }
        } catch (IOException e) {
            logger.warn("Error reading FFmpeg output", e);
        }
    }

    private double parseTimeToSeconds(String hours, String minutes, String seconds) {
        return Integer.parseInt(hours) * 3600 + 
               Integer.parseInt(minutes) * 60 + 
               Double.parseDouble(seconds);
    }

    private void processPlaylistFile(String playlistPath, String videoId) throws IOException {
        Scanner scanner = new Scanner(new File(playlistPath));
        List<String> lines = new ArrayList<>();
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            lines.add(line.replaceAll(videoId + "(\\d+\\.ts)", "segments/" + videoId + "$1"));
        }
        scanner.close();
        
        FileWriter writer = new FileWriter(playlistPath);
        for (String line : lines) {
            writer.write(line + System.lineSeparator());
        }
        writer.close();
    }
}