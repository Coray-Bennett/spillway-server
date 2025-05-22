package com.coraybennett.spillway.service.api;

import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;

import org.springframework.data.jpa.domain.Specification;

/**
 * Service for managing video and playlist access permissions.
 * This centralizes access control logic for easy future extension.
 */
public interface VideoAccessService {
    
    /**
     * Checks if a user has access to a specific video.
     * 
     * @param video The video to check access for
     * @param user The user requesting access (null for anonymous)
     * @return true if the user can access the video
     */
    boolean canAccessVideo(Video video, User user);
    
    /**
     * Checks if a user has access to a specific playlist.
     * 
     * @param playlist The playlist to check access for
     * @param user The user requesting access (null for anonymous)
     * @return true if the user can access the playlist
     */
    boolean canAccessPlaylist(Playlist playlist, User user);
    
    /**
     * Creates a specification that filters videos to only those accessible by the user.
     * 
     * @param user The user to filter for (null for anonymous)
     * @return Specification for accessible videos
     */
    Specification<Video> getVideoAccessSpecification(User user);
    
    /**
     * Creates a specification that filters playlists to only those accessible by the user.
     * 
     * @param user The user to filter for (null for anonymous)
     * @return Specification for accessible playlists
     */
    Specification<Playlist> getPlaylistAccessSpecification(User user);
}