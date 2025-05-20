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
import java.util.HashMap;
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
 * Generalized FFmpeg-based implementation of VideoConversionService.
 * Supports multiple video file formats and adaptive bitrate streaming with better performance.
 * Uses separate FFmpeg processes for each quality level and supports any number of quality levels.
 */
@Service
@Primary
public class GeneralizedFFmpegVideoConversionService implements VideoConversionService {
    private static final Logger logger = LoggerFactory.getLogger(GeneralizedFFmpegVideoConversionService.class);
    
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final Map<String, Process> activeConversions = new ConcurrentHashMap<>();
    
    private final String outputDirectory;
    
    // Supported video file formats
    private final List<String> SUPPORTED_VIDEO_EXTENSIONS = Arrays.asList(
        ".mp4", ".mov", ".avi", ".mkv", ".webm", ".flv", ".wmv", ".m4v"
    );

    // Video resolution patterns for detecting source resolution
    private static final Pattern RESOLUTION_PATTERN = Pattern.compile("Stream .* Video:.* (\\d+)x(\\d+)[,\\s]");
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;
    
    @Value("${video.encoding.preset:veryfast}")
    private String encodingPreset;
    
    @Value("${video.encoding.segment-duration:4}")
    private int segmentDuration;
    
    @Value("${video.encoding.use-hardware-acceleration:false}")
    private boolean useHardwareAcceleration;
    
    @Value("${video.encoding.hardware-accelerator:auto}")
    private String hardwareAccelerator;

    // Quality definitions
    private static class QualityLevel {
        final String name;
        final int height;
        final int width;
        final String bitrate;
        final String maxRate;
        final String bufSize;
        final String audioBitrate;
        final int bandwidth;
        
        QualityLevel(String name, int height, int width, String bitrate, String maxRate, String bufSize, 
                    String audioBitrate, int bandwidth) {
            this.name = name;
            this.height = height;
            this.width = width;
            this.bitrate = bitrate;
            this.maxRate = maxRate;
            this.bufSize = bufSize;
            this.audioBitrate = audioBitrate;
            this.bandwidth = bandwidth;
        }
    }
    
    // Define all possible quality levels
    private static final QualityLevel[] ALL_QUALITY_LEVELS = {
        new QualityLevel("2160p", 2160, 3840, "8000k", "8500k", "12000k", "192k", 8500000),
        new QualityLevel("1080p", 1080, 1920, "5000k", "5350k", "7500k", "192k", 5350000),
        new QualityLevel("720p", 720, 1280, "2500k", "2675k", "3750k", "128k", 2675000),
        new QualityLevel("480p", 480, 854, "1000k", "1075k", "1500k", "128k", 1075000),
        new QualityLevel("360p", 360, 640, "500k", "538k", "750k", "96k", 538000)
    };

    @Autowired
    public GeneralizedFFmpegVideoConversionService(
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
            
            // Ensure output directory exists
            outputPath = Paths.get(getOutputDirectory().toString(), video.getId());
            Files.createDirectories(outputPath);
            
            logger.info("Starting video analysis for video: {}", video.getId());
            
            // First, analyze the video to get its resolution
            int[] resolution = getVideoResolution(sourceFile.toAbsolutePath().toString());
            if (resolution == null) {
                throw new VideoConversionException("Failed to determine video resolution");
            }
            
            int sourceWidth = resolution[0];
            int sourceHeight = resolution[1];
            logger.info("Source video resolution: {}x{}", sourceWidth, sourceHeight);
            
            // Determine appropriate quality levels based on source resolution
            List<QualityLevel> targetQualityLevels = getTargetQualityLevels(sourceWidth, sourceHeight);
            
            if (targetQualityLevels.isEmpty()) {
                throw new VideoConversionException("No suitable quality levels found for source resolution");
            }
            
            // Total number of quality levels to process
            int totalQualityLevels = targetQualityLevels.size();
            int processedQualityLevels = 0;
            
            // Process each quality level in a separate FFmpeg command
            for (QualityLevel quality : targetQualityLevels) {
                logger.info("Processing quality level {} ({} of {})", 
                           quality.name, processedQualityLevels + 1, totalQualityLevels);
                
                // Update progress to reflect quality level processing
                int baseProgress = (processedQualityLevels * 100) / totalQualityLevels;
                int maxProgress = ((processedQualityLevels + 1) * 100) / totalQualityLevels;
                
                convertQuality(
                    sourceFile.toAbsolutePath().toString(),
                    outputPath.toAbsolutePath().toString(),
                    video.getId(),
                    quality,
                    video,
                    baseProgress,
                    maxProgress - 1 // Leave room for completion
                );
                
                processedQualityLevels++;
            }
            
            // Create the master playlist that references all quality variants
            createMasterPlaylist(outputPath.toAbsolutePath().toString(), video.getId(), targetQualityLevels);
            
            // Update video status
            video.setConversionStatus(ConversionStatus.COMPLETED);
            video.setConversionProgress(100);
            video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, video.getId()));
            videoRepository.save(video);
            
            logger.info("Completed FFmpeg conversion for video: {}", video.getId());
            
            // Delete input file after processing
            storageService.delete(sourceFile);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (VideoConversionException | IOException | InterruptedException e) {
            logger.error("Error during video conversion: {}", e.getMessage(), e);
            
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
     * Gets appropriate quality levels based on source resolution.
     * This method ensures we don't create quality levels higher than the source.
     */
    private List<QualityLevel> getTargetQualityLevels(int sourceWidth, int sourceHeight) {
        List<QualityLevel> targetQualityLevels = new ArrayList<>();
        int maxDimension = Math.max(sourceWidth, sourceHeight);
        
        for (QualityLevel quality : ALL_QUALITY_LEVELS) {
            // Only include quality levels that are equal to or lower than the source resolution
            if (quality.height <= maxDimension) {
                targetQualityLevels.add(quality);
            }
        }
        
        // Always ensure at least one quality level (the lowest one if nothing else matches)
        if (targetQualityLevels.isEmpty() && ALL_QUALITY_LEVELS.length > 0) {
            targetQualityLevels.add(ALL_QUALITY_LEVELS[ALL_QUALITY_LEVELS.length - 1]);
        }
        
        return targetQualityLevels;
    }
    
    /**
     * Checks if the video file type is supported for conversion.
     */
    public boolean isVideoFileTypeSupported(String filename) {
        return SUPPORTED_VIDEO_EXTENSIONS.stream()
                .anyMatch(ext -> filename.toLowerCase().endsWith(ext));
    }
    
    /**
     * Gets the resolution of the source video.
     */
    private int[] getVideoResolution(String videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffprobe");
            command.add("-v");
            command.add("error");
            command.add("-show_entries");
            command.add("stream=width,height");
            command.add("-of");
            command.add("default=noprint_wrappers=1");
            command.add(videoPath);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int width = 0;
            int height = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("width=")) {
                    width = Integer.parseInt(line.substring(6));
                } else if (line.startsWith("height=")) {
                    height = Integer.parseInt(line.substring(7));
                }
                
                if (width > 0 && height > 0) {
                    break;
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0 || width == 0 || height == 0) {
                // Try alternative approach if ffprobe failed or didn't return valid dimensions
                return getVideoResolutionAlternative(videoPath);
            }
            
            return new int[] { width, height };
            
        } catch (Exception e) {
            logger.warn("Error determining video resolution with ffprobe: {}", e.getMessage());
            return getVideoResolutionAlternative(videoPath);
        }
    }
    
    /**
     * Alternative method to get video resolution if ffprobe fails.
     */
    private int[] getVideoResolutionAlternative(String videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(videoPath);
            command.add("-hide_banner");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                Matcher matcher = RESOLUTION_PATTERN.matcher(line);
                if (matcher.find()) {
                    int width = Integer.parseInt(matcher.group(1));
                    int height = Integer.parseInt(matcher.group(2));
                    return new int[] { width, height };
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            logger.error("Error determining video resolution with alternative method: {}", e.getMessage());
        }
        
        // Default to 480p if we couldn't determine resolution
        return new int[] { 854, 480 };
    }
    
    /**
     * Converts a single quality level using FFmpeg.
     * Updates progress within the specified range (baseProgress to maxProgress).
     */
    private void convertQuality(String sourceFile, String outputDir, String videoId, 
                              QualityLevel quality, Video video,
                              int baseProgress, int maxProgress) 
            throws IOException, InterruptedException, VideoConversionException {
        
        // Build FFmpeg command for this quality
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(sourceFile);
        command.add("-preset");
        command.add(encodingPreset);
        
        // Add hardware acceleration if enabled
        if (useHardwareAcceleration) {
            addHardwareAcceleration(command);
        }
        
        // Add video encoding parameters
        command.add("-c:v");
        command.add("libx264");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(quality.audioBitrate);
        
        // Add scaling parameters - use -2 to maintain aspect ratio
        command.add("-vf");
        command.add("scale=-2:" + quality.height);
        
        // Add video bitrate parameters
        command.add("-b:v");
        command.add(quality.bitrate);
        command.add("-maxrate");
        command.add(quality.maxRate);
        command.add("-bufsize");
        command.add(quality.bufSize);
        
        // Add HLS parameters
        command.add("-hls_time");
        command.add(String.valueOf(segmentDuration));
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_segment_filename");
        command.add(Paths.get(outputDir, quality.name + "_%03d.ts").toString());
        command.add(Paths.get(outputDir, quality.name + ".m3u8").toString());
        
        logger.info("FFmpeg command for {}: {}", quality.name, String.join(" ", command));
        
        // Execute FFmpeg command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        activeConversions.put(videoId, process);
        
        // Parse FFmpeg output to track progress
        parseFFmpegOutputWithProgressRange(process, video, baseProgress, maxProgress);
        
        // Wait for process to complete
        int exitCode = process.waitFor();
        activeConversions.remove(videoId);
        
        // Check if the conversion succeeded
        if (exitCode != 0) {
            throw new VideoConversionException("FFmpeg conversion for " + quality.name + " failed with exit code: " + exitCode);
        }
        
        // Verify the output file exists
        Path playlistPath = Paths.get(outputDir, quality.name + ".m3u8");
        if (!Files.exists(playlistPath)) {
            throw new VideoConversionException("Conversion failed: " + quality.name + " playlist file not found");
        }
        
        // Process the playlist to update segment URLs
        processPlaylistFile(playlistPath.toString(), videoId, quality.name + "_");
        
        logger.info("{} HLS playlist created successfully at {}", quality.name, playlistPath);
    }

    /**
     * Helper method to add hardware acceleration options to the command.
     */
    private void addHardwareAcceleration(List<String> command) {
        // Select the appropriate hardware accelerator
        if ("auto".equals(hardwareAccelerator)) {
            // Try to auto-detect - NVIDIA GPU gets priority
            try {
                ProcessBuilder nvidiaSmiProcess = new ProcessBuilder("nvidia-smi");
                Process process = nvidiaSmiProcess.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    // NVIDIA GPU detected
                    command.add("-hwaccel");
                    command.add("cuda");
                    command.add("-hwaccel_output_format");
                    command.add("cuda");
                } else {
                    // Try Intel QuickSync
                    command.add("-hwaccel");
                    command.add("qsv");
                }
            } catch (Exception e) {
                // Fallback to software encoding if detection fails
                logger.warn("Hardware acceleration detection failed: {}", e.getMessage());
            }
        } else {
            // Use specified accelerator
            command.add("-hwaccel");
            command.add(hardwareAccelerator);
            
            if ("cuda".equals(hardwareAccelerator)) {
                command.add("-hwaccel_output_format");
                command.add("cuda");
            }
        }
    }

    /**
     * Parses FFmpeg output to track conversion progress within a specified range.
     * Maps FFmpeg's 0-100% to baseProgress-maxProgress.
     */
    private void parseFFmpegOutputWithProgressRange(Process process, Video video, int baseProgress, int maxProgress) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        Pattern durationPattern = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)");
        Pattern progressPattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)");
        
        double totalSeconds = 0;
        String line;
        
        try {
            while ((line = reader.readLine()) != null) {
                // Log occasionally for debugging
                if (Math.random() < 0.05) {
                    logger.debug("FFmpeg output: {}", line);
                }
                
                Matcher durationMatcher = durationPattern.matcher(line);
                if (durationMatcher.find()) {
                    totalSeconds = parseTimeToSeconds(
                        durationMatcher.group(1),
                        durationMatcher.group(2),
                        durationMatcher.group(3)
                    );
                    logger.info("Video duration: {} seconds", totalSeconds);
                }
                
                Matcher progressMatcher = progressPattern.matcher(line);
                if (progressMatcher.find() && totalSeconds > 0) {
                    double currentSeconds = parseTimeToSeconds(
                        progressMatcher.group(1),
                        progressMatcher.group(2),
                        progressMatcher.group(3)
                    );
                    
                    // Calculate ffmpeg's native progress (0-100%)
                    int ffmpegProgress = (int) ((currentSeconds / totalSeconds) * 100);
                    
                    // Map to our progress range (baseProgress-maxProgress)
                    int scaledProgress = baseProgress + (ffmpegProgress * (maxProgress - baseProgress) / 100);
                    
                    // Update progress only if it has changed significantly
                    if (scaledProgress % 5 == 0 && scaledProgress != video.getConversionProgress()) {
                        video.setConversionProgress(scaledProgress);
                        videoRepository.save(video);
                        logger.debug("Conversion progress: {}%", scaledProgress);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Error reading FFmpeg output", e);
        }
    }

    /**
     * Creates a master playlist that references all quality variants with fully qualified URLs.
     */
    private void createMasterPlaylist(String outputDirectory, String videoId, List<QualityLevel> qualities) 
            throws IOException {
        Path masterPlaylistPath = Paths.get(outputDirectory, videoId + ".m3u8");
        
        List<String> masterPlaylistContent = new ArrayList<>();
        masterPlaylistContent.add("#EXTM3U");
        masterPlaylistContent.add("#EXT-X-VERSION:3");
        
        // Add entries for each quality level
        for (QualityLevel quality : qualities) {
            Path qualityPlaylist = Paths.get(outputDirectory, quality.name + ".m3u8");
            
            if (Files.exists(qualityPlaylist)) {
                masterPlaylistContent.add(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d", 
                        quality.bandwidth, quality.width, quality.height));
                masterPlaylistContent.add(String.format("%s/video/%s/playlist/%s", baseUrl, videoId, quality.name));
            }
        }
        
        Files.write(masterPlaylistPath, masterPlaylistContent);
        logger.info("Created master playlist at {} with {} quality levels", masterPlaylistPath, qualities.size());
    }
    
    /**
     * Process a playlist file to update segment URLs.
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
        
        logger.info("Processed playlist at {}", path);
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
            try {
                storageService.delete(inputPath);
                logger.info("Deleted input file: {}", inputPath);
            } catch (Exception e) {
                logger.warn("Failed to delete input file: {}", inputPath);
            }
        }
        
        if (outputPath != null) {
            try {
                storageService.delete(outputPath);
                logger.info("Deleted output directory: {}", outputPath);
            } catch (Exception e) {
                logger.warn("Failed to delete output directory: {}", outputPath);
            }
        }
    }
}