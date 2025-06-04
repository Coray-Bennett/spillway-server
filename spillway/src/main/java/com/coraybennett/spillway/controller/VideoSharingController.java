package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredVideoResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.annotation.Loggable.LogLevel;
import com.coraybennett.spillway.dto.VideoShareRequest;
import com.coraybennett.spillway.dto.VideoShareResponse;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.VideoSharingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for handling video sharing operations.
 * Provides endpoints for sharing videos with other users, managing shares, and querying shared content.
 */
@RestController
@RequestMapping("/video/sharing")
@Slf4j
@RequiredArgsConstructor
public class VideoSharingController {
    
    private final VideoSharingService videoSharingService;
    
    /**
     * Share a video with another user.
     * POST /video/sharing
     */
    @PostMapping
    @Loggable(level = LogLevel.INFO, entryMessage = "Video sharing request received", includeParameters = true)
    @UserAction
    public ResponseEntity<?> shareVideo(
        @Valid @RequestBody VideoShareRequest request, 
        @CurrentUser User user
    ) {
        try {
            VideoShareResponse response = videoSharingService.shareVideo(request, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Video sharing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during video sharing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to share video");
        }
    }
    
    /**
     * Get all shares created by the current user (videos they've shared).
     * GET /video/sharing/created-by-me
     */
    @GetMapping("/created-by-me")
    @Loggable(entryMessage = "Get user shares", includeParameters = true)
    @UserAction
    public ResponseEntity<List<VideoShareResponse>> getMyCreatedShares(@CurrentUser User user) {
        List<VideoShareResponse> shares = videoSharingService.getSharesCreatedBy(user);
        return ResponseEntity.ok(shares);
    }
    
    /**
     * Get all active shares for the current user (videos shared with them).
     * GET /video/sharing/shared-with-me
     */
    @GetMapping("/shared-with-me")
    @Loggable(entryMessage = "Get videos shared with user", includeParameters = true)
    @UserAction
    public ResponseEntity<List<VideoShareResponse>> getSharedWithMe(@CurrentUser User user) {
        List<VideoShareResponse> shares = videoSharingService.getActiveSharesForUser(user);
        return ResponseEntity.ok(shares);
    }
    
    /**
     * Get all shares for a specific video (must be video owner).
     * GET /video/sharing/video/{videoId}
     */
    @GetMapping("/video/{videoId}")
    @SecuredVideoResource
    @Loggable
    @UserAction
    public ResponseEntity<?> getSharesForVideo(
        @PathVariable("videoId") String videoId,
        @ResolvedResource Video video,
        @CurrentUser User user
    ) {
        try {
            List<VideoShareResponse> shares = videoSharingService.getSharesForVideo(video, user);
            
            return ResponseEntity.ok(shares);
        } catch (IllegalArgumentException e) {
            log.warn("Access denied for video shares: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error getting video shares", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get video shares");
        }
    }
    
    /**
     * Get a specific share by ID.
     * GET /video/sharing/{shareId}
     */
    @GetMapping("/{shareId}")
    @Loggable
    @UserAction
    public ResponseEntity<?> getShare(@PathVariable String shareId, 
                                     @CurrentUser User user) {
        Optional<VideoShareResponse> shareOpt = videoSharingService.getShare(shareId);
        if (shareOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        VideoShareResponse share = shareOpt.get();
        
        // Check if user is involved in the share (either sharer or sharee)
        if (!share.getSharedByUsername().equals(user.getUsername()) && 
            !share.getSharedWithUsername().equals(user.getUsername())) {
            log.warn("User {} attempted to access share {} they're not involved in", 
                     user.getUsername(), shareId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(share);
    }
    
    /**
     * Revoke a video share.
     * DELETE /video/sharing/{shareId}
     */
    @DeleteMapping("/{shareId}")
    @Loggable(level = LogLevel.INFO)
    @UserAction
    public ResponseEntity<?> revokeShare(@PathVariable String shareId, 
                                        @CurrentUser User user) {
        try {
            boolean revoked = videoSharingService.revokeShare(shareId, user);
            
            if (revoked) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Share revocation denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during share revocation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to revoke share");
        }
    }
}
