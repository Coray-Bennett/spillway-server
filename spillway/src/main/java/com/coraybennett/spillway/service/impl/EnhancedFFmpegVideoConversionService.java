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
import java.util.Arrays;
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
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.VideoConversionService;

/**
 * Enhanced FFmpeg-based implementation of VideoConversionService.
 * Supports multiple video file formats and adaptive bitrate streaming.
 */
@Service
public class EnhancedFFmpegVideoConversionService implements VideoConversionService {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedFFmpegVideoConversionService.class);
    
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final Map<String, Process> activeConversions = new ConcurrentHashMap<>();
    
    private final String outputDirectory;
    
    // Supported video file formats
    private final List<String> SUPPORTED_VIDEO_EXTENSIONS = Arrays.asList(
        ".mp4", ".mov", ".avi", ".mkv", ".webm", ".flv", ".wmv", ".m4v"
    );
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;
    
    @Value("${video.encoding.max-quality:720p}")
    private String maxQuality;
    
    @Value("${video.encoding.min-quality:360p}")
    private String minQuality;
    
    @Value("${video.encoding.include-480p:true}")
    private boolean include480p;
    
    @Value("${video.encoding.segment-duration:4}")
    private int segmentDuration;
    
    @Value("${video.encoding.high-bitrate:2500k}")
    private String highBitrate;
    
    @Value("${video.encoding.medium-bitrate:1000k}")
    private String mediumBitrate;
    
    @Value("${video.encoding.low-bitrate:500k}")
    private String lowBitrate;

    @Autowired
    public EnhancedFFmpegVideoConversionService(
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
            // Validate file type
            String filename = sourceFile.getFileName().toString();
            if (!isVideoFileTypeSupported(filename)) {
                throw new VideoConversionException("Unsupported video file format: " + filename);
            }
            
            video.setConversionStatus(ConversionStatus.IN_PROGRESS);
            videoRepository.save(video);
            
            outputPath = Paths.get(getOutputDirectory().toString(), video.getId());
            Files.createDirectories(outputPath);
            
            logger.info("Starting FFmpeg conversion for video: {}", video.getId());
            
            // Generate all quality variants
            List<String> command = buildFfmpegCommand(
                sourceFile.toAbsolutePath().toString(), 
                outputPath.toAbsolutePath().toString(),
                video.getId()
            );
            
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
            
            // Create the master playlist that references the quality variants
            createMasterPlaylist(outputPath.toAbsolutePath().toString(), video.getId());
            
            // Process quality variant playlists to update segment URLs
            processQualityPlaylists(outputPath.toAbsolutePath().toString(), video.getId());
            
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
    
    /**
     * Checks if the video file type is supported for conversion.
     */
    public boolean isVideoFileTypeSupported(String filename) {
        return SUPPORTED_VIDEO_EXTENSIONS.stream()
                .anyMatch(ext -> filename.toLowerCase().endsWith(ext));
    }
    
    /**
     * Builds the FFmpeg command for multibitrate HLS conversion.
     */
    protected List<String> buildFfmpegCommand(String inputPath, String outputDir, String videoId) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputPath);
        
        // Add high quality rendition (720p)
        if ("720p".equals(maxQuality) || "1080p".equals(maxQuality)) {
            command.add("-vf");
            command.add("scale=w=1280:h=720:force_original_aspect_ratio=decrease");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("h264");
            command.add("-profile:v");
            command.add("main");
            command.add("-crf");
            command.add("20");
            command.add("-sc_threshold");
            command.add("0");
            command.add("-g");
            command.add("48");
            command.add("-keyint_min");
            command.add("48");
            command.add("-b:v");
            command.add(highBitrate);
            command.add("-maxrate");
            command.add("2675k");
            command.add("-bufsize");
            command.add("3750k");
            command.add("-b:a");
            command.add("128k");
            command.add("-hls_time");
            command.add(String.valueOf(segmentDuration));
            command.add("-hls_playlist_type");
            command.add("vod");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "720p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "720p.m3u8").toString());
        }
        
        // Add medium quality rendition (480p)
        if (include480p) {
            command.add("-vf");
            command.add("scale=w=842:h=480:force_original_aspect_ratio=decrease");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("h264");
            command.add("-profile:v");
            command.add("main");
            command.add("-crf");
            command.add("22");
            command.add("-sc_threshold");
            command.add("0");
            command.add("-g");
            command.add("48");
            command.add("-keyint_min");
            command.add("48");
            command.add("-b:v");
            command.add(mediumBitrate);
            command.add("-maxrate");
            command.add("1075k");
            command.add("-bufsize");
            command.add("1500k");
            command.add("-b:a");
            command.add("128k");
            command.add("-hls_time");
            command.add(String.valueOf(segmentDuration));
            command.add("-hls_playlist_type");
            command.add("vod");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "480p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "480p.m3u8").toString());
        }
        
        // Add low quality rendition (360p)
        if ("360p".equals(minQuality) || "480p".equals(minQuality)) {
            command.add("-vf");
            command.add("scale=w=640:h=360:force_original_aspect_ratio=decrease");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("h264");
            command.add("-profile:v");
            command.add("main");
            command.add("-crf");
            command.add("23");
            command.add("-sc_threshold");
            command.add("0");
            command.add("-g");
            command.add("48");
            command.add("-keyint_min");
            command.add("48");
            command.add("-b:v");
            command.add(lowBitrate);
            command.add("-maxrate");
            command.add("538k");
            command.add("-bufsize");
            command.add("750k");
            command.add("-b:a");
            command.add("96k");
            command.add("-hls_time");
            command.add(String.valueOf(segmentDuration));
            command.add("-hls_playlist_type");
            command.add("vod");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "360p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "360p.m3u8").toString());
        }
        
        return command;
    }

    /**
     * Creates a master playlist that references all quality variants.
     */
    private void createMasterPlaylist(String outputDirectory, String videoId) throws IOException {
        Path masterPlaylistPath = Paths.get(outputDirectory, videoId + ".m3u8");
        
        List<String> masterPlaylistContent = new ArrayList<>();
        masterPlaylistContent.add("#EXTM3U");
        masterPlaylistContent.add("#EXT-X-VERSION:3");
        
        // Add high quality variant
        if ("720p".equals(maxQuality) || "1080p".equals(maxQuality)) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=2628000,RESOLUTION=1280x720");
            masterPlaylistContent.add("720p.m3u8");
        }
        
        // Add medium quality variant
        if (include480p) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=1128000,RESOLUTION=842x480");
            masterPlaylistContent.add("480p.m3u8");
        }
        
        // Add low quality variant
        if ("360p".equals(minQuality) || "480p".equals(minQuality)) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=596000,RESOLUTION=640x360");
            masterPlaylistContent.add("360p.m3u8");
        }
        
        Files.write(masterPlaylistPath, masterPlaylistContent);
    }
    
    /**
     * Process all quality variant playlists to update segment URLs.
     */
    private void processQualityPlaylists(String outputDir, String videoId) throws IOException {
        // Process high quality playlist
        if ("720p".equals(maxQuality) || "1080p".equals(maxQuality)) {
            processPlaylistFile(Paths.get(outputDir, "720p.m3u8").toString(), videoId, "720p_");
        }
        
        // Process medium quality playlist
        if (include480p) {
            processPlaylistFile(Paths.get(outputDir, "480p.m3u8").toString(), videoId, "480p_");
        }
        
        // Process low quality playlist
        if ("360p".equals(minQuality) || "480p".equals(minQuality)) {
            processPlaylistFile(Paths.get(outputDir, "360p.m3u8").toString(), videoId, "360p_");
        }
    }

    /**
     * Process a single quality variant playlist to update segment URLs.
     */
    private void processPlaylistFile(String path, String videoId, String segmentPrefix) throws IOException {
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
