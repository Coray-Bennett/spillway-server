package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.Loggable.LogLevel;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredPlaylistResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.dto.PlaylistCreateRequest;
import com.coraybennett.spillway.dto.PlaylistResponse;
import com.coraybennett.spillway.dto.PlaylistUpdateRequest;
import com.coraybennett.spillway.dto.PlaylistVideoAddResponse;
import com.coraybennett.spillway.dto.PlaylistVideoDetails;
import com.coraybennett.spillway.dto.VideoListResponse;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.PlaylistService;
import com.coraybennett.spillway.service.api.VideoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller handling playlist operations.
 */
@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
@Slf4j
public class PlaylistController {
    private final PlaylistService playlistService;
    private final VideoService videoService;

    /**
     * Create a new playlist.
     * Accepts PlaylistCreateRequest DTO and returns PlaylistResponse DTO.
     */
    @PostMapping
    @UserAction
    @Loggable(level = LogLevel.INFO, entryMessage = "Create playlist", includeParameters = true, includeResult = true)
    public ResponseEntity<PlaylistResponse> createPlaylist(
            @Valid @RequestBody PlaylistCreateRequest request, 
            @CurrentUser User user) {
        try {
            Playlist createdPlaylist = playlistService.createPlaylist(
                request.getName(), 
                request.getDescription(), 
                user
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(new PlaylistResponse(createdPlaylist));
        } catch (Exception e) {
            log.error("Error creating playlist for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get playlist details.
     * Returns PlaylistResponse DTO.
     */
    @GetMapping("/{id}")
    @SecuredPlaylistResource(optionalAuth = true)
    @Loggable(level = LogLevel.DEBUG, entryMessage = "Get playlist", includeResult = true)
    public ResponseEntity<PlaylistResponse> getPlaylist(
            @PathVariable("id") String id, 
            @CurrentUser(required = false) User user,
            @ResolvedResource Playlist playlist) {
        
        return ResponseEntity.ok(new PlaylistResponse(playlist));
    }

    /**
     * Get videos in a playlist.
     * Returns a list of VideoListResponse DTOs.
     */
    @GetMapping("/{id}/videos")
    @SecuredPlaylistResource(optionalAuth = true)
    @Loggable(entryMessage = "Get playlist videos", includeParameters = true)
    public ResponseEntity<List<VideoListResponse>> getPlaylistVideos(
            @PathVariable("id") String id, 
            @CurrentUser(required = false) User user,
            @ResolvedResource Playlist playlist) {
        
        try {
            List<Video> videos = playlistService.getPlaylistVideos(id);
            List<VideoListResponse> videoResponses = videos.stream()
                .map(VideoListResponse::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(videoResponses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all playlists owned by the current user.
     * Returns a list of PlaylistResponse DTOs.
     */
    @GetMapping("/my-playlists")
    @UserAction
    @Loggable(entryMessage = "Get user playlists", includeParameters = true)
    public ResponseEntity<List<PlaylistResponse>> getMyPlaylists(@CurrentUser User user) {
        List<Playlist> playlists = playlistService.listPlaylists(user.getId());
        List<PlaylistResponse> playlistResponses = playlists.stream()
            .map(PlaylistResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(playlistResponses);
    }

    /**
     * Update playlist details.
     * Accepts PlaylistUpdateRequest DTO and returns PlaylistResponse DTO.
     */
    @PutMapping("/{id}")
    @SecuredPlaylistResource(requireWrite = true)
    @Loggable(entryMessage = "Update playlist", includeResult = true)
    public ResponseEntity<PlaylistResponse> updatePlaylist(
            @PathVariable("id") String id, 
            @Valid @RequestBody PlaylistUpdateRequest updateRequest, 
            @CurrentUser User user,
            @ResolvedResource Playlist playlist) {
        
        try {
            // Update the playlist with new details
            playlist.setName(updateRequest.getName());
            playlist.setDescription(updateRequest.getDescription());
            
            // Save the updated playlist
            Playlist updatedPlaylist = playlistService.updatePlaylist(playlist);
            
            return ResponseEntity.ok(new PlaylistResponse(updatedPlaylist));
        } catch (Exception e) {
            log.error("Error updating playlist {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Add a video to a playlist.
     * Returns success status message.
     */
    @PostMapping("/{playlistId}/videos/{videoId}")
    @SecuredPlaylistResource(requireWrite = true, idParameter = "playlistId")
    @Loggable(entryMessage = "Add video to playlist", includeParameters = true)
    public ResponseEntity<PlaylistVideoAddResponse> addVideoToPlaylist(
            @PathVariable("playlistId") String playlistId, 
            @PathVariable("videoId") String videoId,
            @RequestBody(required = false) PlaylistVideoDetails details,
            @CurrentUser User user,
            @ResolvedResource Playlist playlist) {
        
        try {
            Optional<Video> videoOpt = videoService.getVideoById(videoId);
            
            if (videoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Video video = videoOpt.get();
            
            // Check if the user owns the video
            if (!video.getUploadedBy().getId().equals(user.getId())) {
                log.warn("User {} attempted to add video {} they don't own to playlist {}", 
                         user.getUsername(), videoId, playlistId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Add video to playlist
            playlistService.addVideoToPlaylist(playlistId, videoId, details);
            
            PlaylistVideoAddResponse response = new PlaylistVideoAddResponse(
                playlistId, 
                videoId, 
                "Video successfully added to playlist"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding video {} to playlist {}: {}", 
                      videoId, playlistId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove a video from a playlist.
     * Returns success status.
     */
    @DeleteMapping("/{playlistId}/videos/{videoId}")
    @SecuredPlaylistResource(requireWrite = true, idParameter = "playlistId")
    @Loggable(entryMessage = "Remove video from playlist", includeParameters = true)
    public ResponseEntity<Void> removeVideoFromPlaylist(
            @PathVariable("playlistId") String playlistId, 
            @PathVariable("videoId") String videoId, 
            @CurrentUser User user,
            @ResolvedResource Playlist playlist) {
        
        try {
            playlistService.removeVideoFromPlaylist(playlistId, videoId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
