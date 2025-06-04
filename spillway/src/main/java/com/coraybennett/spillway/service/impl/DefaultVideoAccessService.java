package com.coraybennett.spillway.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoSharingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced implementation of VideoAccessService with sharing support.
 * Supports both owned content and shared content access.
 */
@Service
@Slf4j
public class DefaultVideoAccessService implements VideoAccessService {
    
    private final VideoSharingService videoSharingService;
    
    @Autowired
    public DefaultVideoAccessService(VideoSharingService videoSharingService) {
        this.videoSharingService = videoSharingService;
    }
    
    @Override
    public boolean canAccessVideo(Video video, User user) {
        if (video == null || user == null) {
            log.debug("Access denied: video or user is null");
            return false;
        }
        
        // Check if user owns the video
        if (video.getUploadedBy() != null && 
            video.getUploadedBy().getId().equals(user.getId())) {
            log.debug("User {} has access to video {} (owner)", user.getUsername(), video.getId());
            return true;
        }
        
        // Check if video is shared with the user
        boolean isShared = videoSharingService.isVideoSharedWith(video, user);
        if (isShared) {
            log.debug("User {} has access to video {} (shared)", user.getUsername(), video.getId());
            return true;
        }
        
        log.debug("User {} does not have access to video {}", user.getUsername(), video.getId());
        return false;
    }
    
    @Override
    public boolean canAccessPlaylist(Playlist playlist, User user) {
        if (playlist == null || user == null) {
            log.debug("Access denied: playlist or user is null");
            return false;
        }
        
        // Check if user owns the playlist
        if (playlist.getCreatedBy() != null && 
            playlist.getCreatedBy().getId().equals(user.getId())) {
            log.debug("User {} has access to playlist {} (owner)", user.getUsername(), playlist.getId());
            return true;
        }
        
        // TODO: In the future, check sharing permissions for playlists
        log.debug("User {} does not have access to playlist {}", user.getUsername(), playlist.getId());
        return false;
    }
    
    @Override
    public Specification<Video> getVideoAccessSpecification(User user) {
        return (root, query, criteriaBuilder) -> {
            if (user == null) {
                // Anonymous users can't see any videos
                log.debug("Anonymous user - no video access");
                return criteriaBuilder.disjunction(); // Always false
            }
            
            // Videos owned by the user
            var ownedVideos = criteriaBuilder.equal(root.get("uploadedBy").get("id"), user.getId());
            
            // Videos shared with the user (via active and valid shares)
            var sharedVideosSubquery = query.subquery(String.class);
            var shareRoot = sharedVideosSubquery.from(com.coraybennett.spillway.model.VideoShare.class);
            sharedVideosSubquery.select(shareRoot.get("video").get("id"));
            sharedVideosSubquery.where(
                criteriaBuilder.and(
                    criteriaBuilder.equal(shareRoot.get("sharedWith").get("id"), user.getId()),
                    criteriaBuilder.equal(shareRoot.get("active"), true),
                    criteriaBuilder.or(
                        criteriaBuilder.isNull(shareRoot.get("expiresAt")),
                        criteriaBuilder.greaterThan(shareRoot.get("expiresAt"), 
                                                   java.time.LocalDateTime.now())
                    )
                )
            );
            var sharedVideos = root.get("id").in(sharedVideosSubquery);
            
            // Return videos that are either owned by the user or shared with them
            log.debug("Building video access specification for user: {}", user.getUsername());
            return criteriaBuilder.or(ownedVideos, sharedVideos);
        };
    }
    
    @Override
    public Specification<Playlist> getPlaylistAccessSpecification(User user) {
        return (root, query, criteriaBuilder) -> {
            if (user == null) {
                // Anonymous users can't see any playlists
                log.debug("Anonymous user - no playlist access");
                return criteriaBuilder.disjunction(); // Always false
            }
            
            // Currently, only show playlists created by the user
            // TODO: In the future, add OR conditions for shared playlists
            log.debug("Building playlist access specification for user: {}", user.getUsername());
            return criteriaBuilder.equal(root.get("createdBy").get("id"), user.getId());
        };
    }
}