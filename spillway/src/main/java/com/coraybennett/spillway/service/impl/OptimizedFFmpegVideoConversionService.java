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
 * Optimized FFmpeg-based implementation of VideoConversionService.
 * Supports multiple video file formats and adaptive bitrate streaming with better performance.
 */
@Service
@Primary
public class OptimizedFFmpegVideoConversionService implements VideoConversionService {
    private static final Logger logger = LoggerFactory.getLogger(OptimizedFFmpegVideoConversionService.class);
    
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

    @Autowired
    public OptimizedFFmpegVideoConversionService(
            VideoRepository videoRepository, 
            StorageService storageService,
            @Value("${video.output-directory:content}") String outputDirectory) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Converts a video using separate FFmpeg commands for each quality level.
     * This ensures each quality level is properly encoded for the full duration.
     */
    @Override
    @Async("videoConversionExecutor")
    public CompletableFuture<Void> convertToHls(Path sourceFile, Video video) {
        Path outputPath = null;
        
        try {
            // Set status to in progress
            video.setConversionStatus(ConversionStatus.IN_PROGRESS);
            videoRepository.save(video);
            
            // Ensure output directory exists
            outputPath = Paths.get(getOutputDirectory().toString(), video.getId());
            Files.createDirectories(outputPath);
            
            logger.info("Starting video analysis for video: {}", video.getId());
            
            // Analyze the video to get its resolution
            int[] resolution = getVideoResolution(sourceFile.toAbsolutePath().toString());
            if (resolution == null) {
                throw new VideoConversionException("Failed to determine video resolution");
            }
            
            int sourceWidth = resolution[0];
            int sourceHeight = resolution[1];
            logger.info("Source video resolution: {}x{}", sourceWidth, sourceHeight);
            
            // Determine which quality levels to create
            boolean[] qualityLevels = determineQualityLevels(sourceWidth, sourceHeight);
            boolean include480p = qualityLevels[3];
            boolean include360p = qualityLevels[4];
            
            // Process 480p if needed
            if (include480p) {
                logger.info("Starting 480p conversion");
                convertQuality(sourceFile.toAbsolutePath().toString(), 
                            outputPath.toString(), 
                            video.getId(),
                            "480", 854, 480, "1000k");
            }
            
            // Process 360p if needed
            if (include360p) {
                logger.info("Starting 360p conversion");
                convertQuality(sourceFile.toAbsolutePath().toString(), 
                            outputPath.toString(), 
                            video.getId(),
                            "360", 640, 360, "500k");
            }
            
            // Create the master playlist
            createMasterPlaylist(outputPath.toAbsolutePath().toString(), 
                                video.getId(), 
                                sourceWidth, 
                                sourceHeight);
            
            // Update video status
            video.setConversionStatus(ConversionStatus.COMPLETED);
            video.setConversionProgress(100);
            video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, video.getId()));
            videoRepository.save(video);
            
            logger.info("Completed FFmpeg conversion for video: {}", video.getId());
            
            // Delete input file after processing
            storageService.delete(sourceFile);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Error during video conversion: {}", e.getMessage(), e);
            
            cleanupOnError(sourceFile, outputPath);
            
            video.setConversionStatus(ConversionStatus.FAILED);
            video.setConversionError(e.getMessage());
            videoRepository.save(video);
            
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Converts a single quality level using FFmpeg.
     */
    private void convertQuality(String sourceFile, String outputDir, String videoId, 
                            String qualityName, int width, int height, String bitrate) 
            throws IOException, InterruptedException, VideoConversionException {
        
        // Build FFmpeg command for this quality
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(sourceFile);
        command.add("-preset");
        command.add(encodingPreset);
        command.add("-c:v");
        command.add("libx264");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-vf");
        command.add("scale=" + width + ":" + height);
        command.add("-b:v");
        command.add(bitrate);
        command.add("-maxrate");
        command.add(bitrate);
        command.add("-bufsize");
        command.add(bitrate);
        command.add("-hls_time");
        command.add("4");
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_segment_filename");
        command.add(Paths.get(outputDir, qualityName + "p_%03d.ts").toString());
        command.add(Paths.get(outputDir, qualityName + "p.m3u8").toString());
        
        logger.info("FFmpeg command for {}p: {}", qualityName, String.join(" ", command));
        
        // Execute FFmpeg command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        // Track progress
        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = outputReader.readLine()) != null) {
            if (Math.random() < 0.1) {
                logger.debug("FFmpeg {}p output: {}", qualityName, line);
            }
        }
        
        // Wait for process to complete
        int exitCode = process.waitFor();
        
        // Check if the conversion succeeded
        if (exitCode != 0) {
            throw new VideoConversionException("FFmpeg conversion for " + qualityName + "p failed with exit code: " + exitCode);
        }
        
        // Verify the output files exist
        Path playlist = Paths.get(outputDir, qualityName + "p.m3u8");
        if (!Files.exists(playlist)) {
            throw new VideoConversionException("Conversion failed: " + qualityName + "p playlist file not found");
        }
        
        // Process the playlist to update URLs
        processPlaylistFile(playlist.toString(), videoId, qualityName + "p_");
        
        logger.info("{}p HLS playlist created successfully", qualityName);
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
        
        // Default to 1080p if we couldn't determine resolution
        return new int[] { 1920, 1080 };
    }
    
    /**
     * Determine which quality renditions to create based on source resolution.
     * Returns array of [include2160p, include1080p, include720p, include480p, include360p]
     */
    private boolean[] determineQualityLevels(int width, int height) {
        boolean include2160p = false;
        boolean include1080p = false;
        boolean include720p = false;
        boolean include480p = true;  // Always include 480p for mid-range devices
        boolean include360p = true;  // Always include 360p for mobile/low bandwidth
        
        int maxDimension = Math.max(width, height);
        
        // For 4K content (3840x2160 or higher)
        if (maxDimension >= 3840) {
            include2160p = true;
            include1080p = true;
            include720p = true;
        }
        // For 1080p content (1920x1080 or higher, but less than 4K)
        else if (maxDimension >= 1920) {
            include1080p = true;
            include720p = true;
        }
        // For 720p content (1280x720 or higher, but less than 1080p)
        else if (maxDimension >= 1280) {
            include720p = true;
        }
        // For SD content (less than 720p)
        else if (maxDimension < 720) {
            // Don't create renditions higher than source
            include720p = false;
            
            // If even less than 480p
            if (maxDimension < 480) {
                include480p = false;
            }
        }
        
        return new boolean[] { include2160p, include1080p, include720p, include480p, include360p };
    }
    
    /**
     * Builds the optimized FFmpeg command for HLS conversion based on source resolution.
     * Fixed parameter ordering issue.
     */
    protected List<String> buildOptimizedFfmpegCommand(String inputPath, String outputDir, String videoId, int sourceWidth, int sourceHeight) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputPath);
        
        // Add preset parameter FIRST before any mapping
        command.add("-preset");
        command.add(encodingPreset);  // Options: ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow
        
        // Use hardware acceleration if enabled
        if (useHardwareAcceleration) {
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

        // Add HLS common parameters
        command.add("-hls_time");
        command.add(String.valueOf(segmentDuration));
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_flags");
        command.add("independent_segments");

        // Get quality levels based on source resolution
        boolean[] qualityLevels = determineQualityLevels(sourceWidth, sourceHeight);
        boolean include2160p = qualityLevels[0];
        boolean include1080p = qualityLevels[1];
        boolean include720p = qualityLevels[2];
        boolean include480p = qualityLevels[3];
        boolean include360p = qualityLevels[4];

        // For a simpler approach, we'll create a single output at a time
        List<String> finalCommand = new ArrayList<>(command);
        
        // For lower resolution sources, we'll just add the appropriate quality levels
        if (include480p) {
            finalCommand.add("-vf");
            finalCommand.add("scale=-2:480");
            finalCommand.add("-c:a");
            finalCommand.add("aac");
            finalCommand.add("-ar");
            finalCommand.add("48000");
            finalCommand.add("-c:v");
            finalCommand.add("libx264");
            finalCommand.add("-profile:v");
            finalCommand.add("main");
            finalCommand.add("-crf");
            finalCommand.add("23");
            finalCommand.add("-sc_threshold");
            finalCommand.add("0");
            finalCommand.add("-g");
            finalCommand.add("48");
            finalCommand.add("-keyint_min");
            finalCommand.add("48");
            finalCommand.add("-b:v");
            finalCommand.add("1000k");
            finalCommand.add("-maxrate");
            finalCommand.add("1075k");
            finalCommand.add("-bufsize");
            finalCommand.add("1500k");
            finalCommand.add("-b:a");
            finalCommand.add("128k");
            finalCommand.add("-hls_segment_filename");
            finalCommand.add(Paths.get(outputDir, "480p_%03d.ts").toString());
            finalCommand.add(Paths.get(outputDir, "480p.m3u8").toString());
        }
        
        if (include360p) {
            finalCommand.add("-vf");
            finalCommand.add("scale=-2:360");
            finalCommand.add("-c:a");
            finalCommand.add("aac");
            finalCommand.add("-ar");
            finalCommand.add("48000");
            finalCommand.add("-c:v");
            finalCommand.add("libx264");
            finalCommand.add("-profile:v");
            finalCommand.add("main");
            finalCommand.add("-crf");
            finalCommand.add("23");
            finalCommand.add("-sc_threshold");
            finalCommand.add("0");
            finalCommand.add("-g");
            finalCommand.add("48");
            finalCommand.add("-keyint_min");
            finalCommand.add("48");
            finalCommand.add("-b:v");
            finalCommand.add("500k");
            finalCommand.add("-maxrate");
            finalCommand.add("538k");
            finalCommand.add("-bufsize");
            finalCommand.add("750k");
            finalCommand.add("-b:a");
            finalCommand.add("96k");
            finalCommand.add("-hls_segment_filename");
            finalCommand.add(Paths.get(outputDir, "360p_%03d.ts").toString());
            finalCommand.add(Paths.get(outputDir, "360p.m3u8").toString());
        }

        return finalCommand;
    }

    /**
     * Creates a master playlist that references all quality variants with fully qualified URLs.
     */
    private void createMasterPlaylist(String outputDirectory, String videoId, int sourceWidth, int sourceHeight) throws IOException {
        Path masterPlaylistPath = Paths.get(outputDirectory, videoId + ".m3u8");
        
        boolean[] qualityLevels = determineQualityLevels(sourceWidth, sourceHeight);
        boolean include2160p = qualityLevels[0];
        boolean include1080p = qualityLevels[1];
        boolean include720p = qualityLevels[2];
        boolean include480p = qualityLevels[3];
        boolean include360p = qualityLevels[4];
        
        List<String> masterPlaylistContent = new ArrayList<>();
        masterPlaylistContent.add("#EXTM3U");
        masterPlaylistContent.add("#EXT-X-VERSION:3");
        
        // Add 2160p quality variant if included
        if (include2160p && Files.exists(Paths.get(outputDirectory, "2160p.m3u8"))) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=8500000,RESOLUTION=3840x2160");
            masterPlaylistContent.add(String.format("%s/video/%s/playlist/2160p", baseUrl, videoId));
        }
        
        // Add 1080p quality variant if included
        if (include1080p && Files.exists(Paths.get(outputDirectory, "1080p.m3u8"))) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=5350000,RESOLUTION=1920x1080");
            masterPlaylistContent.add(String.format("%s/video/%s/playlist/1080p", baseUrl, videoId));
        }
        
        // Add 720p quality variant if included
        if (include720p && Files.exists(Paths.get(outputDirectory, "720p.m3u8"))) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=2675000,RESOLUTION=1280x720");
            masterPlaylistContent.add(String.format("%s/video/%s/playlist/720p", baseUrl, videoId));
        }
        
        // Add 480p quality variant if included
        if (include480p && Files.exists(Paths.get(outputDirectory, "480p.m3u8"))) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=1075000,RESOLUTION=854x480");
            masterPlaylistContent.add(String.format("%s/video/%s/playlist/480p", baseUrl, videoId));
        }
        
        // Add 360p quality variant if included
        if (include360p && Files.exists(Paths.get(outputDirectory, "360p.m3u8"))) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=538000,RESOLUTION=640x360");
            masterPlaylistContent.add(String.format("%s/video/%s/playlist/360p", baseUrl, videoId));
        }
        
        Files.write(masterPlaylistPath, masterPlaylistContent);
        logger.info("Created master playlist at {} with fully qualified URLs", masterPlaylistPath);
    }
    
    /**
     * Process all quality variant playlists to update segment URLs.
     */
    private void processQualityPlaylists(String outputDir, String videoId, int sourceWidth, int sourceHeight) throws IOException {
        boolean[] qualityLevels = determineQualityLevels(sourceWidth, sourceHeight);
        boolean include2160p = qualityLevels[0];
        boolean include1080p = qualityLevels[1];
        boolean include720p = qualityLevels[2];
        boolean include480p = qualityLevels[3];
        boolean include360p = qualityLevels[4];
        
        // Process 2160p playlist if it exists
        if (include2160p) {
            Path playlist2160p = Paths.get(outputDir, "2160p.m3u8");
            if (Files.exists(playlist2160p)) {
                processPlaylistFile(playlist2160p.toString(), videoId, "2160p_");
            }
        }
        
        // Process 1080p playlist if it exists
        if (include1080p) {
            Path playlist1080p = Paths.get(outputDir, "1080p.m3u8");
            if (Files.exists(playlist1080p)) {
                processPlaylistFile(playlist1080p.toString(), videoId, "1080p_");
            }
        }
        
        // Process 720p playlist if it exists
        if (include720p) {
            Path playlist720p = Paths.get(outputDir, "720p.m3u8");
            if (Files.exists(playlist720p)) {
                processPlaylistFile(playlist720p.toString(), videoId, "720p_");
            }
        }
        
        // Process 480p playlist if it exists
        if (include480p) {
            Path playlist480p = Paths.get(outputDir, "480p.m3u8");
            if (Files.exists(playlist480p)) {
                processPlaylistFile(playlist480p.toString(), videoId, "480p_");
            }
        }
        
        // Process 360p playlist if it exists
        if (include360p) {
            Path playlist360p = Paths.get(outputDir, "360p.m3u8");
            if (Files.exists(playlist360p)) {
                processPlaylistFile(playlist360p.toString(), videoId, "360p_");
            }
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
        
        logger.info("Processed playlist at {}", path);
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
                // Log every 10th line for debugging
                if (Math.random() < 0.1) {
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
                    
                    int progress = (int) ((currentSeconds / totalSeconds) * 100);
                    logger.debug("Conversion progress: {}%", progress);
                    
                    // Update progress only if it has changed significantly (every 5%)
                    if (progress % 5 == 0 && progress != video.getConversionProgress()) {
                        video.setConversionProgress(Math.min(progress, 99)); // Cap at 99% until complete
                        videoRepository.save(video);
                    }
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