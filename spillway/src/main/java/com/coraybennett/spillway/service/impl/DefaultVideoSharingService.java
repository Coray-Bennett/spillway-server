package com.coraybennett.spillway.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coraybennett.spillway.dto.VideoShareRequest;
import com.coraybennett.spillway.dto.VideoShareResponse;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoShare;
import com.coraybennett.spillway.model.VideoShare.SharePermission;
import com.coraybennett.spillway.repository.VideoShareRepository;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoService;
import com.coraybennett.spillway.service.api.VideoSharingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of video sharing service.
 */
@Service
@Transactional
@Slf4j
public class DefaultVideoSharingService implements VideoSharingService {
    
    private final VideoShareRepository videoShareRepository;
    private final VideoService videoService;
    private final UserService userService;
    
    @Autowired
    public DefaultVideoSharingService(VideoShareRepository videoShareRepository,
                                      VideoService videoService,
                                      UserService userService) {
        this.videoShareRepository = videoShareRepository;
        this.videoService = videoService;
        this.userService = userService;
    }
    
    @Override
    public VideoShareResponse shareVideo(VideoShareRequest request, User sharedBy) {
        log.info("User {} attempting to share video {} with {}", 
                 sharedBy.getUsername(), request.getVideoId(), request.getSharedWithUsername());
        
        // Validate video exists and user owns it
        Video video = videoService.getVideoById(request.getVideoId())
            .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        
        if (!video.getUploadedBy().getId().equals(sharedBy.getId())) {
            log.warn("User {} attempted to share video {} they don't own", 
                     sharedBy.getUsername(), request.getVideoId());
            throw new IllegalArgumentException("You can only share videos you own");
        }
        
        // Validate target user exists
        User sharedWith = userService.findByUsername(request.getSharedWithUsername())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getSharedWithUsername()));
        
        // Check if already shared with this user
        Optional<VideoShare> existingShare = videoShareRepository
            .findByVideoIdAndSharedByIdAndSharedWithId(video.getId(), sharedBy.getId(), sharedWith.getId());
        
        if (existingShare.isPresent()) {
            VideoShare existing = existingShare.get();
            if (existing.isActive()) {
                log.info("Video {} already shared with user {}, updating permissions", 
                         video.getId(), sharedWith.getUsername());
                // Update existing share
                existing.setPermission(request.getPermission());
                existing.setExpiresAt(request.getExpiresAt());
                VideoShare updated = videoShareRepository.save(existing);
                return new VideoShareResponse(updated);
            } else {
                // Reactivate existing share
                existing.setActive(true);
                existing.setPermission(request.getPermission());
                existing.setExpiresAt(request.getExpiresAt());
                VideoShare reactivated = videoShareRepository.save(existing);
                log.info("Reactivated video share {} for user {}", video.getId(), sharedWith.getUsername());
                return new VideoShareResponse(reactivated);
            }
        }
        
        // Create new share
        VideoShare newShare = shareVideo(video, sharedBy, sharedWith, 
                                         request.getPermission(), request.getExpiresAt());
        log.info("Successfully shared video {} with user {}", video.getId(), sharedWith.getUsername());
        return new VideoShareResponse(newShare);
    }
    
    @Override
    public VideoShare shareVideo(Video video, User sharedBy, User sharedWith, 
                                 SharePermission permission, LocalDateTime expiresAt) {
        if (!video.getUploadedBy().getId().equals(sharedBy.getId())) {
            throw new IllegalArgumentException("You can only share videos you own");
        }
        
        VideoShare share = new VideoShare(video, sharedBy, sharedWith, permission, expiresAt);
        return videoShareRepository.save(share);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VideoShareResponse> getSharesCreatedBy(User user) {
        log.debug("Getting shares created by user: {}", user.getUsername());
        return videoShareRepository.findBySharedByUsernameOrderByCreatedAtDesc(user.getUsername())
                .stream()
                .map(VideoShareResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VideoShareResponse> getSharesForUser(User user) {
        log.debug("Getting all shares for user: {}", user.getUsername());
        return videoShareRepository.findBySharedWithUsernameOrderByCreatedAtDesc(user.getUsername())
                .stream()
                .map(VideoShareResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VideoShareResponse> getActiveSharesForUser(User user) {
        log.debug("Getting active shares for user: {}", user.getUsername());
        return videoShareRepository.findAllValidSharesForUser(user.getId())
                .stream()
                .map(VideoShareResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isVideoSharedWith(Video video, User user) {
        if (video == null || user == null) {
            return false;
        }
        return videoShareRepository.isVideoSharedWithUser(video.getId(), user.getId());
    }
    
    @Override
    public boolean revokeShare(String shareId, User revokedBy) {
        log.info("User {} attempting to revoke share {}", revokedBy.getUsername(), shareId);
        
        Optional<VideoShare> shareOpt = videoShareRepository.findById(shareId);
        if (shareOpt.isEmpty()) {
            log.warn("Share {} not found for revocation", shareId);
            return false;
        }
        
        VideoShare share = shareOpt.get();
        
        // Check if user has permission to revoke (must be the sharer)
        if (!share.getSharedBy().getId().equals(revokedBy.getId())) {
            log.warn("User {} attempted to revoke share {} they didn't create", 
                     revokedBy.getUsername(), shareId);
            throw new IllegalArgumentException("You can only revoke shares you created");
        }
        
        share.setActive(false);
        videoShareRepository.save(share);
        log.info("Successfully revoked share {} by user {}", shareId, revokedBy.getUsername());
        return true;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<VideoShareResponse> getShare(String shareId) {
        return videoShareRepository.findById(shareId)
                .map(VideoShareResponse::new);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VideoShareResponse> getSharesForVideo(Video video, User requestedBy) {
        // Verify user owns the video
        if (!video.getUploadedBy().getId().equals(requestedBy.getId())) {
            throw new IllegalArgumentException("You can only view shares for videos you own");
        }
        
        log.debug("Getting shares for video {} requested by {}", video.getId(), requestedBy.getUsername());
        return videoShareRepository.findByVideoId(video.getId())
                .stream()
                .map(VideoShareResponse::new)
                .collect(Collectors.toList());
    }
}