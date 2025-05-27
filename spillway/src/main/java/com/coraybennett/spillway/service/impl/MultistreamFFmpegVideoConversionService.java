package com.coraybennett.spillway.service.impl;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import com.coraybennett.spillway.service.enums.QualityLevel;

/**
 * FFmpeg-based implementation of VideoConversionService.
 * Supports multiple video file formats and adaptive bitrate streaming with better performance.
 * Uses separate FFmpeg processes for each quality level and supports any number of quality levels.
 */
@Service
@Primary
public class MultistreamFFmpegVideoConversionService implements VideoConversionService {
    private static final Logger logger = LoggerFactory.getLogger(MultistreamFFmpegVideoConversionService.class);
    
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final Map<String, Process> activeConversions = new ConcurrentHashMap<>();
    
    private final String outputDirectory;
    
    // Supported video file formats
    private static final List<String> SUPPORTED_VIDEO_EXTENSIONS = Arrays.asList(
        ".mp4", ".mov", ".avi", ".mkv", ".webm", ".flv", ".wmv", ".m4v"
    );

    // Video resolution patterns for detecting source resolution
    private static final Pattern RESOLUTION_PATTERN = Pattern.compile("Stream .* Video:.* (\\d+)x(\\d+)[,\\s]");
    
    // FFmpeg progress patterns
    private static final Pattern DURATION_PATTERN = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)");
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)");
    
    // Timeout constants
    private static final int FFPROBE_TIMEOUT_SECONDS = 30;
    private static final int HWACC_TEST_TIMEOUT_SECONDS = 20;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;
    
    @Value("${video.encoding.preset:veryfast}")
    private String encodingPreset;
    
    @Value("${video.encoding.segment-duration:4}")
    private int segmentDuration;

    @Value("${video.encoding.enable-hw-accel:false}")
    private boolean hardwareAccelerationEnabled;
    
    @Value("${video.encoding.ffmpeg-timeout-minutes:120}")
    private int ffmpegTimeoutMinutes;
    
    @Value("${video.encoding.parallel-quality-conversion:false}")
    private boolean parallelQualityConversion;

    // Hardware acceleration cache to avoid repeated checks
    private String cachedHardwareAcceleration = null;
    private boolean hwAccelChecked = false;
    
    private static final QualityLevel[] ALL_QUALITY_LEVELS = QualityLevel.ALL_QUALITY_LEVELS;

    @Autowired
    public MultistreamFFmpegVideoConversionService(
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
            
            if (parallelQualityConversion && totalQualityLevels > 1) {
                // Process quality levels in parallel for faster conversion
                processQualityLevelsInParallel(sourceFile, outputPath, video, targetQualityLevels);
            } else {
                // Process each quality level sequentially
                processQualityLevelsSequentially(sourceFile, outputPath, video, targetQualityLevels);
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

    /**
     * Process quality levels sequentially with progress tracking
     */
    private void processQualityLevelsSequentially(
            Path sourceFile, 
            Path outputPath, 
            Video video, 
            List<QualityLevel> targetQualityLevels) throws IOException, InterruptedException, VideoConversionException {
        
        int totalQualityLevels = targetQualityLevels.size();
        int processedQualityLevels = 0;
        
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
    }

    /**
     * Process quality levels in parallel with properly managed progress tracking
     */
    private void processQualityLevelsInParallel(
            Path sourceFile, 
            Path outputPath, 
            Video video, 
            List<QualityLevel> targetQualityLevels) throws IOException, InterruptedException, VideoConversionException {
        
        String sourceFilePath = sourceFile.toAbsolutePath().toString();
        String outputPathString = outputPath.toAbsolutePath().toString();
        String videoId = video.getId();
        
        // Create a list of futures for parallel processing
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int totalQualityLevels = targetQualityLevels.size();
        
        // Progress tracking for parallel conversion
        // Each quality level contributes equally to the total progress
        final Map<String, Integer> qualityProgress = new ConcurrentHashMap<>();
        final AtomicInteger overallProgress = new AtomicInteger(0);
        
        // Initialize progress map
        for (QualityLevel quality : targetQualityLevels) {
            qualityProgress.put(quality.name, 0);
        }
        
        // Start each quality conversion as a separate CompletableFuture
        for (int i = 0; i < totalQualityLevels; i++) {
            QualityLevel quality = targetQualityLevels.get(i);
            
            logger.info("Starting parallel conversion for quality level {} ({} of {})", 
                quality.name, i + 1, totalQualityLevels);
            
            // Launch each conversion in a separate thread
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    convertQualityWithProgressCallback(
                        sourceFilePath,
                        outputPathString,
                        videoId,
                        quality,
                        video,
                        (progress) -> {
                            // Update quality-specific progress
                            qualityProgress.put(quality.name, progress);
                            
                            // Calculate overall progress as the average of all quality levels
                            int totalProgress = qualityProgress.values().stream()
                                .mapToInt(Integer::intValue)
                                .sum();
                            int avgProgress = totalProgress / totalQualityLevels;
                            
                            // Only update if progress increased
                            int currentOverall = overallProgress.get();
                            if (avgProgress > currentOverall) {
                                overallProgress.set(avgProgress);
                                video.setConversionProgress(avgProgress);
                                videoRepository.save(video);
                                logger.debug("Overall conversion progress: {}%", avgProgress);
                            }
                        }
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Failed to convert quality " + quality.name, e);
                }
            });
            
            futures.add(future);
        }
        
        // Wait for all conversions to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            // Handle any conversion failures
            throw new VideoConversionException("Failed to convert one or more quality levels: " + e.getMessage(), e);
        }
        
        logger.info("All quality levels processed in parallel for video: {}", videoId);
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
            
            boolean completed = process.waitFor(FFPROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.warn("ffprobe timed out when determining duration");
                return 0;
            }
            
            int exitCode = process.exitValue();
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
     * Gets the resolution of the source video with standardized timeout handling
     */
    private int[] getVideoResolution(String videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffprobe");
            command.add("-v");
            command.add("error");
            command.add("-show_entries");
            command.add("stream=width,height");
            command.add("-select_streams");
            command.add("v:0"); // Select only the first video stream
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
            
            boolean completed = process.waitFor(FFPROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.warn("ffprobe timed out when determining resolution");
                return getVideoResolutionAlternative(videoPath);
            }
            
            int exitCode = process.exitValue();
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
        ProcessBuilder processBuilder = null;
        Process process = null;
        
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(videoPath);
            command.add("-hide_banner");
            
            processBuilder = new ProcessBuilder(command);
            process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            
            long startTime = System.currentTimeMillis();
            while ((line = reader.readLine()) != null) {
                // Check for timeout
                if (System.currentTimeMillis() - startTime > FFPROBE_TIMEOUT_SECONDS * 1000) {
                    break;
                }
                
                Matcher matcher = RESOLUTION_PATTERN.matcher(line);
                if (matcher.find()) {
                    int width = Integer.parseInt(matcher.group(1));
                    int height = Integer.parseInt(matcher.group(2));
                    return new int[] { width, height };
                }
            }
        } catch (Exception e) {
            logger.error("Error determining video resolution with alternative method: {}", e.getMessage());
        } finally {
            // Ensure process is cleaned up
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        
        // Default to 480p if we couldn't determine resolution
        return new int[] { 854, 480 };
    }
    
    /**
     * Converts a single quality level using FFmpeg with progress callback for parallel tracking
     */
    private void convertQualityWithProgressCallback(
            String sourceFile, 
            String outputDir, 
            String videoId,
            QualityLevel quality, 
            Video video,
            ProgressCallback progressCallback) 
            throws IOException, InterruptedException, VideoConversionException {
        
        // Build FFmpeg command for this quality
        List<String> command = buildFfmpegCommand(sourceFile, outputDir, quality);
        
        logger.info("FFmpeg command for {}: {}", quality.name, String.join(" ", command));
        
        // Execute FFmpeg command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        activeConversions.put(videoId, process);
        
        try {
            // Parse FFmpeg output with progress callback
            parseFFmpegOutputWithCallback(process, progressCallback);
            
            boolean completed = process.waitFor(ffmpegTimeoutMinutes, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                throw new VideoConversionException("FFmpeg conversion for " + quality.name + " timed out after " + ffmpegTimeoutMinutes + " minutes");
            }
            
            int exitCode = process.exitValue();
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
            
            // Report 100% completion for this quality
            progressCallback.onProgress(100);
            
        } finally {
            activeConversions.remove(videoId);
        }
    }
    
    /**
     * Converts a single quality level using FFmpeg.
     * Updates progress within the specified range (baseProgress to maxProgress).
     */
    private void convertQuality(String sourceFile, String outputDir, String videoId, 
                          QualityLevel quality, Video video,
                          int baseProgress, int maxProgress) 
        throws IOException, InterruptedException, VideoConversionException {
        
        convertQualityWithProgressCallback(sourceFile, outputDir, videoId, quality, video, 
            progress -> {
                // Map progress to range
                int scaledProgress = baseProgress + (progress * (maxProgress - baseProgress) / 100);
                video.setConversionProgress(scaledProgress);
                videoRepository.save(video);
            }
        );
    }

    /**
     * Builds FFmpeg command with appropriate encoding settings
     */
    private List<String> buildFfmpegCommand(String sourceFile, String outputDir, QualityLevel quality) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(sourceFile);
        
        // Only check for hardware acceleration once and cache the result
        String hwAccel = null;
        synchronized (this) {
            if (!hwAccelChecked) {
                cachedHardwareAcceleration = detectHardwareAcceleration();
                hwAccelChecked = true;
            }
            hwAccel = cachedHardwareAcceleration;
        }
        
        if (hwAccel != null) {
            applyHardwareAcceleration(command, hwAccel, quality);
        } else {
            // Use software encoding with optimizations
            command.add("-preset");
            command.add(encodingPreset);
            command.add("-c:v");
            command.add("libx264");
            command.add("-b:v");
            command.add(quality.bitrate);
            command.add("-maxrate");
            command.add(quality.maxRate);
            command.add("-bufsize");
            command.add(quality.bufSize);
        }
        
        // Add audio encoding
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(quality.audioBitrate);
        
        // Add scaling parameters - use -2 to maintain aspect ratio
        if (hwAccel == null || !hwAccel.equals("vaapi")) { // VAAPI has scaling in its filter
            command.add("-vf");
            command.add("scale=-2:" + quality.height);
        }
        
        // Add HLS parameters with optimizations
        command.add("-hls_time");
        command.add(String.valueOf(segmentDuration));
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_segment_type");
        command.add("mpegts");
        command.add("-hls_flags");
        command.add("independent_segments");
        command.add("-hls_segment_filename");
        command.add(Paths.get(outputDir, quality.name + "_%03d.ts").toString());
        command.add("-hls_list_size");
        command.add("0");
        command.add("-movflags");
        command.add("+faststart");
        command.add(Paths.get(outputDir, quality.name + ".m3u8").toString());
        
        return command;
    }

    /**
     * Parses FFmpeg output and reports progress through callback
     */
    private void parseFFmpegOutputWithCallback(Process process, ProgressCallback callback) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        double totalSeconds = 0;
        String line;
        int lastReportedProgress = -1;
        
        try {
            while ((line = reader.readLine()) != null) {
                // Extract duration information
                Matcher durationMatcher = DURATION_PATTERN.matcher(line);
                if (durationMatcher.find() && totalSeconds == 0) {
                    totalSeconds = parseTimeToSeconds(
                        durationMatcher.group(1),
                        durationMatcher.group(2),
                        durationMatcher.group(3)
                    );
                }
                
                // Extract progress information
                Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
                if (progressMatcher.find() && totalSeconds > 0) {
                    double currentSeconds = parseTimeToSeconds(
                        progressMatcher.group(1),
                        progressMatcher.group(2),
                        progressMatcher.group(3)
                    );
                    
                    int progress = Math.min((int) ((currentSeconds / totalSeconds) * 100), 99);
                    
                    // Only report progress if it has changed meaningfully
                    if (progress > lastReportedProgress) {
                        lastReportedProgress = progress;
                        callback.onProgress(progress);
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
        
        // Read all lines and process them in a single pass
        List<String> lines = Files.readAllLines(playlist.toPath());
        
        List<String> processedLines = lines.stream()
            .map(line -> {
                if (line.endsWith(".ts") && !line.startsWith("http")) {
                    return String.format("%s/video/%s/segments/%s", baseUrl, videoId, line);
                } else {
                    return line;
                }
            })
            .collect(Collectors.toList());
        
        // Write back to file
        Files.write(playlist.toPath(), processedLines);
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

    /**
     * Detects available hardware acceleration methods.
     */
    private String detectHardwareAcceleration() {
        if (!hardwareAccelerationEnabled) {
            return null;
        }

        // First, check if there are any hardware encoders available
        Map<String, String> accelEncoders = new HashMap<>();
        accelEncoders.put("nvenc", "h264_nvenc");
        accelEncoders.put("qsv", "h264_qsv");
        accelEncoders.put("vaapi", "h264_vaapi");
        accelEncoders.put("videotoolbox", "h264_videotoolbox");
        
        List<String> availableEncoders = new ArrayList<>();
        
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-encoders");
            command.add("-hide_banner");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            boolean completed = process.waitFor(HWACC_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.warn("Timeout checking hardware encoders");
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                for (String encoder : accelEncoders.values()) {
                    if (line.contains(encoder)) {
                        availableEncoders.add(encoder);
                        logger.info("Found hardware encoder: {}", encoder);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error checking for hardware encoders: {}", e.getMessage());
            return null;
        }
        
        // If no hardware encoders are available, return null
        if (availableEncoders.isEmpty()) {
            logger.info("No hardware acceleration detected, using software encoding");
            return null;
        }
        
        // Try to validate each encoder with a quick test
        for (Map.Entry<String, String> entry : accelEncoders.entrySet()) {
            String accelType = entry.getKey();
            String encoder = entry.getValue();
            
            if (!availableEncoders.contains(encoder)) {
                continue;
            }
            
            if (testHardwareEncoder(accelType, encoder)) {
                logger.info("Hardware acceleration validated: {} using {}", accelType, encoder);
                return accelType;
            }
        }
        
        logger.info("No working hardware acceleration found, falling back to software encoding");
        return null;
    }
    
    /**
     * Tests if a hardware encoder actually works
     */
    private boolean testHardwareEncoder(String accelType, String encoder) {
        Process testProcess = null;
        
        try {
            List<String> testCommand = new ArrayList<>();
            testCommand.add("ffmpeg");
            testCommand.add("-f");
            testCommand.add("lavfi");
            testCommand.add("-i");
            testCommand.add("testsrc=duration=1:size=640x360:rate=30");
            testCommand.add("-c:v");
            testCommand.add(encoder);
            
            // Add specific flags for different acceleration types
            switch (accelType) {
                case "vaapi":
                    testCommand.add("-vaapi_device");
                    testCommand.add("/dev/dri/renderD128");
                    testCommand.add("-vf");
                    testCommand.add("format=nv12|vaapi,hwupload");
                    break;
                    
                case "qsv":
                    testCommand.add("-preset");
                    testCommand.add("faster");
                    break;
                    
                case "nvenc":
                    testCommand.add("-preset");
                    testCommand.add("p4");
                    break;
            }
            
            testCommand.add("-f");
            testCommand.add("null");
            testCommand.add("-");
            
            ProcessBuilder testProcessBuilder = new ProcessBuilder(testCommand);
            testProcessBuilder.redirectErrorStream(true);
            testProcess = testProcessBuilder.start();
            
            boolean testCompleted = testProcess.waitFor(HWACC_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!testCompleted) {
                logger.warn("Hardware acceleration test timed out for {}", accelType);
                return false;
            }
            
            int testExitCode = testProcess.exitValue();
            return testExitCode == 0;
            
        } catch (Exception e) {
            logger.warn("Error testing hardware acceleration {}: {}", accelType, e.getMessage());
            return false;
        } finally {
            if (testProcess != null && testProcess.isAlive()) {
                testProcess.destroyForcibly();
            }
        }
    }

    /**
     * Modify FFmpeg command for hardware acceleration
     */
    private void applyHardwareAcceleration(List<String> command, String acceleration, QualityLevel quality) {
        if (acceleration == null) return;
        
        switch (acceleration) {
            case "nvenc":
                command.add("-c:v");
                command.add("h264_nvenc");
                command.add("-preset");
                command.add("p4");  // Lower values are higher quality but slower
                break;
                
            case "qsv":
                command.add("-c:v");
                command.add("h264_qsv");
                command.add("-preset");
                command.add("faster");
                break;
                
            case "vaapi":
                command.add("-vaapi_device");
                command.add("/dev/dri/renderD128");
                command.add("-vf");
                command.add("format=nv12|vaapi,hwupload");
                command.add("-c:v");
                command.add("h264_vaapi");
                break;
                
            case "videotoolbox":
                command.add("-c:v");
                command.add("h264_videotoolbox");
                command.add("-profile:v");
                command.add("main");
                break;
        }
        
        // Add common parameters
        command.add("-b:v");
        command.add(quality.bitrate);
        command.add("-maxrate");
        command.add(quality.maxRate);
        command.add("-bufsize");
        command.add(quality.bufSize);
    }
    
    /**
     * Callback interface for progress tracking
     */
    private interface ProgressCallback {
        void onProgress(int percentage);
    }
}