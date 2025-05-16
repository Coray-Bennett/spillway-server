package com.coraybennett.spillway.service.api;

import java.util.List;
import java.util.Optional;

import com.coraybennett.spillway.dto.PlaylistVideoDetails;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;

/**
 * Interface defining operations for playlist management.
 */
public interface PlaylistService {
    
    /**
     * Creates a new playlist.
     * 
     * @param name Playlist name
     * @param description Playlist description
     * @param user Owner of the playlist
     * @return The created playlist
     */
    Playlist createPlaylist(String name, String description, User user);
    
    /**
     * Retrieves a playlist by its ID.
     * 
     * @param id Playlist ID
     * @return Optional containing the playlist if found
     */
    Optional<Playlist> getPlaylistById(String id);
    
    /**
     * Lists all playlists, optionally filtered by user.
     * 
     * @param userId Optional user ID to filter playlists
     * @return List of playlists matching the criteria
     */
    List<Playlist> listPlaylists(String userId);
    
    /**
     * Adds a video to a playlist.
     * 
     * @param playlistId ID of the playlist
     * @param videoId ID of the video to add
     * @return Updated playlist
     */
    Playlist addVideoToPlaylist(String playlistId, String videoId);
    
    /**
     * Removes a video from a playlist.
     * 
     * @param playlistId ID of the playlist
     * @param videoId ID of the video to remove
     * @return Updated playlist
     */
    Playlist removeVideoFromPlaylist(String playlistId, String videoId);
    
    /**
     * Gets videos associated with a playlist.
     * 
     * @param playlistId ID of the playlist
     * @return List of video details in the playlist
     */
    List<PlaylistVideoDetails> getPlaylistVideos(String playlistId);
}
