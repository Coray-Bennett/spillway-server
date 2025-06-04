package com.coraybennett.spillway.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coraybennett.spillway.dto.VideoShareRequest;
import com.coraybennett.spillway.dto.VideoShareResponse;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoService;
import com.coraybennett.spillway.service.api.VideoSharingService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for handling video sharing operations.
 * Provides endpoints for sharing videos with other users, managing shares, and querying shared content.
 */
@RestController
@RequestMapping("/video/sharing")
@Slf4j
public class VideoSharingController {
    
    private final VideoSharingService videoSharingService;
    private final UserService userService;
    private final VideoService videoService;
    
    @Autowired
    public VideoSharingController(VideoSharingService videoSharingService,
                                  UserService userService,
                                  VideoService videoService) {
        this.videoSharingService = videoSharingService;
        this.userService = userService;
        this.videoService = videoService;
    }
    
    /**
     * Share a video with another user.
     * POST /video/sharing
     */
    @PostMapping
    public ResponseEntity<?> shareVideo(@Valid @RequestBody VideoShareRequest request, Principal principal) {
        log.info("Video sharing request from user: {}", principal.getName());
        
        try {
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", principal.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            User user = userOpt.get();
            VideoShareResponse response = videoSharingService.shareVideo(request, user);
            
            log.info("Successfully shared video {} with user {}", 
                     request.getVideoId(), request.getSharedWithUsername());
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
    public ResponseEntity<?> getMyCreatedShares(Principal principal) {
        log.debug("Getting shares created by user: {}", principal.getName());
        
        Optional<User> userOpt = userService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", principal.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        User user = userOpt.get();
        List<VideoShareResponse> shares = videoSharingService.getSharesCreatedBy(user);
        
        log.debug("Found {} shares created by user {}", shares.size(), user.getUsername());
        return ResponseEntity.ok(shares);
    }
    
    /**
     * Get all active shares for the current user (videos shared with them).
     * GET /video/sharing/shared-with-me
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<?> getSharedWithMe(Principal principal) {
        log.debug("Getting active shares for user: {}", principal.getName());
        
        Optional<User> userOpt = userService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", principal.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        User user = userOpt.get();
        List<VideoShareResponse> shares = videoSharingService.getActiveSharesForUser(user);
        
        log.debug("Found {} active shares for user {}", shares.size(), user.getUsername());
        return ResponseEntity.ok(shares);
    }
    
    /**
     * Get all shares for a specific video (must be video owner).
     * GET /video/sharing/video/{videoId}
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<?> getSharesForVideo(@PathVariable String videoId, Principal principal) {
        log.debug("Getting shares for video {} requested by {}", videoId, principal.getName());
        
        try {
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", principal.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Optional<Video> videoOpt = videoService.getVideoById(videoId);
            if (videoOpt.isEmpty()) {
                log.warn("Video not found: {}", videoId);
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Video video = videoOpt.get();
            
            List<VideoShareResponse> shares = videoSharingService.getSharesForVideo(video, user);
            
            log.debug("Found {} shares for video {}", shares.size(), videoId);
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
    public ResponseEntity<?> getShare(@PathVariable String shareId, Principal principal) {
        log.debug("Getting share {} requested by {}", shareId, principal.getName());
        
        Optional<User> userOpt = userService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", principal.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<VideoShareResponse> shareOpt = videoSharingService.getShare(shareId);
        if (shareOpt.isEmpty()) {
            log.warn("Share not found: {}", shareId);
            return ResponseEntity.notFound().build();
        }
        
        VideoShareResponse share = shareOpt.get();
        User user = userOpt.get();
        
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
    public ResponseEntity<?> revokeShare(@PathVariable String shareId, Principal principal) {
        log.info("Revoking share {} requested by {}", shareId, principal.getName());
        
        try {
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", principal.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            User user = userOpt.get();
            boolean revoked = videoSharingService.revokeShare(shareId, user);
            
            if (revoked) {
                log.info("Successfully revoked share {} by user {}", shareId, user.getUsername());
                return ResponseEntity.ok().build();
            } else {
                log.warn("Failed to revoke share {}", shareId);
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
