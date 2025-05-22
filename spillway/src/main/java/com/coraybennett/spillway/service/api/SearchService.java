package com.coraybennett.spillway.service.api;

import java.util.List;
import org.springframework.data.domain.Page;

import com.coraybennett.spillway.dto.VideoSearchRequest;
import com.coraybennett.spillway.dto.PlaylistSearchRequest;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;

/**
 * Interface defining search and filter operations.
 */
public interface SearchService {
    
    /**
     * Search videos with advanced filters.
     * 
     * @param request Search request with filters
     * @return Page of videos matching criteria
     */
    Page<Video> searchVideos(VideoSearchRequest request);
    
    /**
     * Search playlists with advanced filters.
     * 
     * @param request Search request with filters
     * @return Page of playlists matching criteria
     */
    Page<Playlist> searchPlaylists(PlaylistSearchRequest request);
    
    /**
     * Get all unique genres from videos.
     * 
     * @return List of genre names
     */
    List<String> getAllGenres();
    
    /**
     * Get recently added videos.
     * 
     * @param limit Maximum number of videos to return
     * @return List of recently added videos
     */
    List<Video> getRecentlyAddedVideos(int limit);
    
    /**
     * Get most popular playlists by video count.
     * 
     * @param limit Maximum number of playlists to return
     * @return List of popular playlists
     */
    List<Playlist> getMostPopularPlaylists(int limit);
}