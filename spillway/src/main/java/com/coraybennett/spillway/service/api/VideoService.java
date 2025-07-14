package com.coraybennett.spillway.service.api;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.dto.VideoResponse;
import com.coraybennett.spillway.dto.VideoUploadRequest;
import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;

/**
 * Interface defining operations for video management.
 */
public interface VideoService {
    
    /**
     * Creates a new video entry with metadata.
     * 
     * @param metadata Video metadata information
     * @param user The user uploading the video
     * @return VideoResponse containing the created video information
     */
    VideoResponse createVideo(VideoUploadRequest metadata, User user);
    
    /**
     * Uploads and processes a video file for a given video ID.
     * 
     * @param videoId ID of the video to associate the file with
     * @param videoFile The multipart file containing the video data
     * @param encryptionKey optional encryption key for encrypted videos
     * @throws VideoConversionException if there is an issue with the video upload or processing
     */
    void uploadAndConvertVideo(String videoId, MultipartFile videoFile, String encryptionKey) 
            throws VideoConversionException;
    
    /**
     * Retrieves a video by its ID.
     * 
     * @param id The video ID to search for
     * @return Optional containing the video if found
     */
    Optional<Video> getVideoById(String id);
    
    /**
     * Gets the progress of a video conversion.
     * 
     * @param id The video ID to check conversion status for
     * @return ConversionProgress object with status information or null if video not found
     */
    ConversionProgress getConversionProgress(String id);
    
    /**
     * Lists all videos, optionally filtered by user.
     * 
     * @param userId Optional user ID to filter videos by uploader
     * @return List of videos matching the criteria
     */
    List<Video> listVideos(String userId);
    
    /**
     * Updates a video with new information.
     * 
     * @param video The video with updated information
     * @return The updated video
     */
    Video updateVideo(Video video);
    
    /**
     * Gets the video conversion service.
     * 
     * @return The video conversion service
     */
    VideoConversionService getVideoConversionService();
    
    /**
     * Record class for tracking video conversion progress.
     */
    public static record ConversionProgress(
        ConversionStatus status,
        Integer progress,
        String error
    ) {}
}