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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

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
            
            // Build optimized FFmpeg command based on source resolution
            List<String> command = buildOptimizedFfmpegCommand(
                sourceFile.toAbsolutePath().toString(), 
                outputPath.toAbsolutePath().toString(),
                video.getId(),
                sourceWidth,
                sourceHeight
            );
            
            logger.info("Starting FFmpeg conversion for video: {}", video.getId());
            
            // Execute FFmpeg command and capture output
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            
            // Redirect error stream for debugging
            processBuilder.redirectErrorStream(true);
            
            // Start the process
            Process process = processBuilder.start();
            
            // Keep track of active process for potential cancellation
            activeConversions.put(video.getId(), process);
            
            // Parse FFmpeg output for progress
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            parseFFmpegOutput(outputReader, video);
            
            // Wait for process to complete
            int exitCode = process.waitFor();
            activeConversions.remove(video.getId());
            
            // Check if the conversion succeeded
            if (exitCode != 0) {
                throw new VideoConversionException("FFmpeg conversion failed with exit code: " + exitCode);
            }
            
            // Verify the output files exist
            Path masterPlaylist = Paths.get(outputPath.toString(), video.getId() + ".m3u8");
            if (!Files.exists(masterPlaylist)) {
                throw new VideoConversionException("Conversion failed: Master playlist file not found");
            }
            
            // Create the master playlist that references the quality variants
            createMasterPlaylist(outputPath.toAbsolutePath().toString(), video.getId(), sourceWidth, sourceHeight);
            
            // Process quality variant playlists to update segment URLs
            processQualityPlaylists(outputPath.toAbsolutePath().toString(), video.getId(), sourceWidth, sourceHeight);
            
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
     */
    protected List<String> buildOptimizedFfmpegCommand(String inputPath, String outputDir, String videoId, int sourceWidth, int sourceHeight) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputPath);
        
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
        
        // Add global encoding settings
        command.add("-preset");
        command.add(encodingPreset);  // Options: ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow
        
        // Get quality levels based on source resolution
        boolean[] qualityLevels = determineQualityLevels(sourceWidth, sourceHeight);
        boolean include2160p = qualityLevels[0];
        boolean include1080p = qualityLevels[1];
        boolean include720p = qualityLevels[2];
        boolean include480p = qualityLevels[3];
        boolean include360p = qualityLevels[4];

        // Create output streaming directory
        command.add("-hls_time");
        command.add(String.valueOf(segmentDuration));
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_flags");
        command.add("independent_segments");
        
        // Create an array to keep track of all output streams for map options
        List<String> mapOptions = new ArrayList<>();
        int outputIndex = 0;
        
        // Add 2160p (4K) quality level if source is 4K or higher
        if (include2160p) {
            command.add("-vf");
            command.add("scale=-2:2160");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("libx264");
            command.add("-profile:v");
            command.add("high");
            command.add("-crf");
            command.add("22");
            command.add("-sc_threshold");
            command.add("0");
            command.add("-g");
            command.add("48");
            command.add("-keyint_min");
            command.add("48");
            command.add("-b:v");
            command.add("8000k");
            command.add("-maxrate");
            command.add("8500k");
            command.add("-bufsize");
            command.add("12000k");
            command.add("-b:a");
            command.add("192k");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "2160p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "2160p.m3u8").toString());
            
            mapOptions.add("-map");
            mapOptions.add("0:v:0");
            mapOptions.add("-map");
            mapOptions.add("0:a:0");
            outputIndex++;
        }
        
        // Add 1080p quality level if source is 1080p or higher
        if (include1080p) {
            command.add("-vf");
            command.add("scale=-2:1080");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("libx264");
            command.add("-profile:v");
            command.add("high");
            command.add("-crf");
            command.add("23");
            command.add("-sc_threshold");
            command.add("0");
            command.add("-g");
            command.add("48");
            command.add("-keyint_min");
            command.add("48");
            command.add("-b:v");
            command.add("5000k");
            command.add("-maxrate");
            command.add("5350k");
            command.add("-bufsize");
            command.add("7500k");
            command.add("-b:a");
            command.add("192k");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "1080p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "1080p.m3u8").toString());
            
            mapOptions.add("-map");
            mapOptions.add("0:v:0");
            mapOptions.add("-map");
            mapOptions.add("0:a:0");
            outputIndex++;
        }
        
        // Add 720p quality level if source is 720p or higher
        if (include720p) {
            command.add("-vf");
            command.add("scale=-2:720");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("libx264");
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
            command.add("2500k");
            command.add("-maxrate");
            command.add("2675k");
            command.add("-bufsize");
            command.add("3750k");
            command.add("-b:a");
            command.add("128k");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "720p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "720p.m3u8").toString());
            
            mapOptions.add("-map");
            mapOptions.add("0:v:0");
            mapOptions.add("-map");
            mapOptions.add("0:a:0");
            outputIndex++;
        }
        
        // Add 480p quality level (always included unless source is lower)
        if (include480p) {
            command.add("-vf");
            command.add("scale=-2:480");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("libx264");
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
            command.add("1000k");
            command.add("-maxrate");
            command.add("1075k");
            command.add("-bufsize");
            command.add("1500k");
            command.add("-b:a");
            command.add("128k");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "480p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "480p.m3u8").toString());
            
            mapOptions.add("-map");
            mapOptions.add("0:v:0");
            mapOptions.add("-map");
            mapOptions.add("0:a:0");
            outputIndex++;
        }
        
        // Add 360p quality level (always included for mobile/low bandwidth)
        if (include360p) {
            command.add("-vf");
            command.add("scale=-2:360");
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-c:v");
            command.add("libx264");
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
            command.add("500k");
            command.add("-maxrate");
            command.add("538k");
            command.add("-bufsize");
            command.add("750k");
            command.add("-b:a");
            command.add("96k");
            command.add("-hls_segment_filename");
            command.add(Paths.get(outputDir, "360p_%03d.ts").toString());
            command.add(Paths.get(outputDir, "360p.m3u8").toString());
            
            mapOptions.add("-map");
            mapOptions.add("0:v:0");
            mapOptions.add("-map");
            mapOptions.add("0:a:0");
        }
        
        // Insert map options before all the output options (after input options)
        int inputParamCount = 2; // -i and the input filepath
        if (useHardwareAcceleration) {
            inputParamCount += 2; // Add hwaccel parameters
            if ("cuda".equals(hardwareAccelerator)) {
                inputParamCount += 2; // Add hwaccel_output_format
            }
        }
        
        for (int i = 0; i < mapOptions.size(); i++) {
            command.add(inputParamCount + i, mapOptions.get(i));
        }

        // Log the complete command for debugging
        logger.info("FFmpeg command: {}", String.join(" ", command));
        
        return command;
    }

    /**
     * Creates a master playlist that references all quality variants.
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
        if (include2160p) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=8500000,RESOLUTION=3840x2160");
            masterPlaylistContent.add("2160p.m3u8");
        }
        
        // Add 1080p quality variant if included
        if (include1080p) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=5350000,RESOLUTION=1920x1080");
            masterPlaylistContent.add("1080p.m3u8");
        }
        
        // Add 720p quality variant if included
        if (include720p) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=2675000,RESOLUTION=1280x720");
            masterPlaylistContent.add("720p.m3u8");
        }
        
        // Add 480p quality variant if included
        if (include480p) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=1075000,RESOLUTION=854x480");
            masterPlaylistContent.add("480p.m3u8");
        }
        
        // Add 360p quality variant if included
        if (include360p) {
            masterPlaylistContent.add("#EXT-X-STREAM-INF:BANDWIDTH=538000,RESOLUTION=640x360");
            masterPlaylistContent.add("360p.m3u8");
        }
        
        Files.write(masterPlaylistPath, masterPlaylistContent);
        logger.info("Created master playlist at {}", masterPlaylistPath);
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