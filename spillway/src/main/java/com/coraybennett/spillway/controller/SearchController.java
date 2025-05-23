package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.dto.*;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.service.api.SearchService;
import com.coraybennett.spillway.service.api.UserService;

/**
 * Refactored controller handling search and filter operations.
 * All searches are restricted to content the authenticated user has access to.
 */
@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;
    private final UserService userService;

    @Autowired
    public SearchController(SearchService searchService, UserService userService) {
        this.searchService = searchService;
        this.userService = userService;
    }

    @PostMapping("/videos")
    @UserAction
    public ResponseEntity<SearchResponse<VideoListResponse>> searchVideos(
            @RequestBody VideoSearchRequest request,
            @CurrentUser User user) {
        
        Page<Video> videoPage = searchService.searchVideos(request, user);
        
        List<VideoListResponse> videoResponses = videoPage.getContent().stream()
            .map(VideoListResponse::new)
            .collect(Collectors.toList());
        
        SearchResponse<VideoListResponse> response = new SearchResponse<>(
            videoResponses,
            videoPage.getTotalElements(),
            videoPage.getTotalPages(),
            videoPage.getNumber(),
            videoPage.getSize()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/playlists")
    @UserAction
    public ResponseEntity<SearchResponse<PlaylistResponse>> searchPlaylists(
            @RequestBody PlaylistSearchRequest request,
            @CurrentUser User user) {
        
        Page<Playlist> playlistPage = searchService.searchPlaylists(request, user);
        
        List<PlaylistResponse> playlistResponses = playlistPage.getContent().stream()
            .map(PlaylistResponse::new)
            .collect(Collectors.toList());
        
        SearchResponse<PlaylistResponse> response = new SearchResponse<>(
            playlistResponses,
            playlistPage.getTotalElements(),
            playlistPage.getTotalPages(),
            playlistPage.getNumber(),
            playlistPage.getSize()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/genres")
    @UserAction
    public ResponseEntity<List<String>> getAllGenres(@CurrentUser User user) {
        List<String> genres = searchService.getAllGenres(user);
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/videos/recent")
    @UserAction
    public ResponseEntity<List<VideoListResponse>> getRecentVideos(
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser User user) {
        
        List<Video> recentVideos = searchService.getRecentlyAddedVideos(limit, user);
        List<VideoListResponse> responses = recentVideos.stream()
            .map(VideoListResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/playlists/popular")
    @UserAction
    public ResponseEntity<List<PlaylistResponse>> getPopularPlaylists(
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser User user) {
        
        List<Playlist> popularPlaylists = searchService.getMostPopularPlaylists(limit, user);
        List<PlaylistResponse> responses = popularPlaylists.stream()
            .map(PlaylistResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/videos/quick")
    @UserAction
    public ResponseEntity<SearchResponse<VideoListResponse>> quickSearchVideos(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser User user) {
        
        VideoSearchRequest request = new VideoSearchRequest();
        request.setQuery(q);
        request.setPage(page);
        request.setSize(size);
        
        return searchVideos(request, user);
    }
}