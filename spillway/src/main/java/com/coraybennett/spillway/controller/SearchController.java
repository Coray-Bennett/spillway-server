package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.dto.*;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.service.api.SearchService;

/**
 * Controller handling search and filter operations.
 */
@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/videos")
    public ResponseEntity<SearchResponse<VideoListResponse>> searchVideos(
            @RequestBody VideoSearchRequest request) {
        Page<Video> videoPage = searchService.searchVideos(request);
        
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
    public ResponseEntity<SearchResponse<PlaylistResponse>> searchPlaylists(
            @RequestBody PlaylistSearchRequest request) {
        Page<Playlist> playlistPage = searchService.searchPlaylists(request);
        
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
    public ResponseEntity<List<String>> getAllGenres() {
        List<String> genres = searchService.getAllGenres();
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/videos/recent")
    public ResponseEntity<List<VideoListResponse>> getRecentVideos(
            @RequestParam(defaultValue = "10") int limit) {
        List<Video> recentVideos = searchService.getRecentlyAddedVideos(limit);
        List<VideoListResponse> responses = recentVideos.stream()
            .map(VideoListResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/playlists/popular")
    public ResponseEntity<List<PlaylistResponse>> getPopularPlaylists(
            @RequestParam(defaultValue = "10") int limit) {
        List<Playlist> popularPlaylists = searchService.getMostPopularPlaylists(limit);
        List<PlaylistResponse> responses = popularPlaylists.stream()
            .map(PlaylistResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/videos/quick")
    public ResponseEntity<SearchResponse<VideoListResponse>> quickSearchVideos(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        VideoSearchRequest request = new VideoSearchRequest();
        request.setQuery(q);
        request.setPage(page);
        request.setSize(size);
        
        return searchVideos(request);
    }
}