package com.coraybennett.spillway.service.api;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.coraybennett.spillway.model.Video;

/**
 * Interface defining operations for video conversion.
 */
public interface VideoConversionService {
    
    /**
     * Converts a video file to HLS format for streaming.
     * 
     * @param sourceFile The path to the source video file
     * @param video The Video entity associated with this file
     * @return CompletableFuture that completes when the conversion is done
     */
    CompletableFuture<Void> convertToHls(Path sourceFile, Video video);
    
    /**
     * Cancels an ongoing video conversion.
     * 
     * @param videoId ID of the video conversion to cancel
     * @return true if cancellation was successful, false otherwise
     */
    boolean cancelConversion(String videoId);
    
    /**
     * Gets the directory where converted videos are stored.
     * 
     * @return Path to the output directory
     */
    Path getOutputDirectory();
    
    /**
     * Deletes all files associated with a converted video.
     * 
     * @param videoId ID of the video to clean up
     * @return true if cleanup was successful, false otherwise
     */
    boolean cleanupVideoFiles(String videoId);
}