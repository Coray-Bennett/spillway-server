package com.coraybennett.spillway.service.impl;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.service.api.VideoAccessService;

import jakarta.persistence.criteria.Predicate;

/**
 * Default implementation of VideoAccessService.
 * Currently only allows access to content uploaded by the user.
 * Future versions can extend this to support sharing.
 */
@Service
public class DefaultVideoAccessService implements VideoAccessService {
    
    @Override
    public boolean canAccessVideo(Video video, User user) {
        if (video == null || user == null) {
            return false;
        }
        
        // Currently, users can only access their own videos
        // TODO: In the future, check sharing permissions here
        return video.getUploadedBy() != null && 
               video.getUploadedBy().getId().equals(user.getId());
    }
    
    @Override
    public boolean canAccessPlaylist(Playlist playlist, User user) {
        if (playlist == null || user == null) {
            return false;
        }
        
        // Currently, users can only access their own playlists
        // TODO: In the future, check sharing permissions here
        return playlist.getCreatedBy() != null && 
               playlist.getCreatedBy().getId().equals(user.getId());
    }
    
    @Override
    public Specification<Video> getVideoAccessSpecification(User user) {
        return (root, query, criteriaBuilder) -> {
            if (user == null) {
                // Anonymous users can't see any videos
                return criteriaBuilder.disjunction(); // Always false
            }
            
            // Currently, only show videos uploaded by the user
            // TODO: In the future, add OR conditions for shared videos
            return criteriaBuilder.equal(root.get("uploadedBy").get("id"), user.getId());
        };
    }
    
    @Override
    public Specification<Playlist> getPlaylistAccessSpecification(User user) {
        return (root, query, criteriaBuilder) -> {
            if (user == null) {
                // Anonymous users can't see any playlists
                return criteriaBuilder.disjunction(); // Always false
            }
            
            // Currently, only show playlists created by the user
            // TODO: In the future, add OR conditions for shared playlists
            return criteriaBuilder.equal(root.get("createdBy").get("id"), user.getId());
        };
    }
}