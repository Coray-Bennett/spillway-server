package com.coraybennett.spillway.service.api;

import java.util.List;
import org.springframework.data.domain.Page;

import com.coraybennett.spillway.dto.VideoSearchRequest;
import com.coraybennett.spillway.dto.PlaylistSearchRequest;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;

/**
 * Interface defining search and filter operations.
 */
public interface SearchService {
    
    /**
     * Search videos with advanced filters, restricted to videos the user has access to.
     * 
     * @param request Search request with filters
     * @param user The user performing the search (null for anonymous)
     * @return Page of videos matching criteria that the user can access
     */
    Page<Video> searchVideos(VideoSearchRequest request, User user);
    
    /**
     * Search playlists with advanced filters, restricted to playlists the user has access to.
     * 
     * @param request Search request with filters
     * @param user The user performing the search (null for anonymous)
     * @return Page of playlists matching criteria that the user can access
     */
    Page<Playlist> searchPlaylists(PlaylistSearchRequest request, User user);
    
    /**
     * Get all unique genres from videos the user has access to.
     * 
     * @param user The user requesting genres (null for anonymous)
     * @return List of genre names
     */
    List<String> getAllGenres(User user);
    
    /**
     * Get recently added videos that the user has access to.
     * 
     * @param limit Maximum number of videos to return
     * @param user The user requesting videos (null for anonymous)
     * @return List of recently added videos
     */
    List<Video> getRecentlyAddedVideos(int limit, User user);
    
    /**
     * Get most popular playlists by video count that the user has access to.
     * 
     * @param limit Maximum number of playlists to return
     * @param user The user requesting playlists (null for anonymous)
     * @return List of popular playlists
     */
    List<Playlist> getMostPopularPlaylists(int limit, User user);
}
