package com.coraybennett.spillway.service.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.coraybennett.spillway.dto.VideoShareRequest;
import com.coraybennett.spillway.dto.VideoShareResponse;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoShare;
import com.coraybennett.spillway.model.VideoShare.SharePermission;

/**
 * Service interface for managing video sharing between users.
 * This service handles creating, managing, and querying video shares.
 */
public interface VideoSharingService {
    
    /**
     * Share a video with another user.
     * 
     * @param request The sharing request details
     * @param sharedBy The user sharing the video
     * @return The created video share
     * @throws IllegalArgumentException if video not found or user doesn't own it
     */
    VideoShareResponse shareVideo(VideoShareRequest request, User sharedBy);
    
    /**
     * Share a video with another user.
     * 
     * @param video The video to share
     * @param sharedBy The user sharing the video
     * @param sharedWith The user to share with
     * @param permission The permission level to grant
     * @param expiresAt Optional expiration date
     * @return The created video share
     * @throws IllegalArgumentException if user doesn't own the video
     */
    VideoShare shareVideo(Video video, User sharedBy, User sharedWith, 
                          SharePermission permission, LocalDateTime expiresAt);
    
    /**
     * Get all shares created by a user (videos they've shared).
     * 
     * @param user The user who shared videos
     * @return List of shares created by the user
     */
    List<VideoShareResponse> getSharesCreatedBy(User user);
    
    /**
     * Get all shares for a user (videos shared with them).
     * 
     * @param user The user who received shared videos
     * @return List of shares for the user
     */
    List<VideoShareResponse> getSharesForUser(User user);
    
    /**
     * Get all active shares for a user (videos shared with them).
     * 
     * @param user The user who received shared videos
     * @return List of valid/active shares for the user
     */
    List<VideoShareResponse> getActiveSharesForUser(User user);
    
    /**
     * Check if a video is shared with a specific user.
     * 
     * @param video The video to check
     * @param user The user to check access for
     * @return true if the video is shared with the user and the share is valid
     */
    boolean isVideoSharedWith(Video video, User user);
    
    /**
     * Revoke a video share.
     * 
     * @param shareId The ID of the share to revoke
     * @param revokedBy The user revoking the share (must be the original sharer)
     * @return true if the share was successfully revoked
     * @throws IllegalArgumentException if share not found or user doesn't have permission
     */
    boolean revokeShare(String shareId, User revokedBy);
    
    /**
     * Get a specific video share by ID.
     * 
     * @param shareId The share ID
     * @return Optional containing the share if found
     */
    Optional<VideoShareResponse> getShare(String shareId);
    
    /**
     * Get all shares for a specific video.
     * 
     * @param video The video to get shares for
     * @param requestedBy The user requesting the information (must be video owner)
     * @return List of shares for the video
     * @throws IllegalArgumentException if user doesn't own the video
     */
    List<VideoShareResponse> getSharesForVideo(Video video, User requestedBy);
}