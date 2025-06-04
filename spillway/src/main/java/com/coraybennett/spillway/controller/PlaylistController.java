package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.Loggable.LogLevel;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredPlaylistResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.dto.PlaylistResponse;
import com.coraybennett.spillway.dto.PlaylistVideoDetails;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.PlaylistService;
import com.coraybennett.spillway.service.api.VideoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fully refactored controller handling playlist operations using meta-annotations for access control.
 */
@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
@Slf4j
public class PlaylistController {
    private final PlaylistService playlistService;
    private final VideoService videoService;

    @PostMapping
    @UserAction
    @Loggable(level = LogLevel.INFO, entryMessage = "Create playlist", includeParameters = true, includeResult = true)
    public ResponseEntity<PlaylistResponse> createPlaylist(@RequestBody Playlist playlist, @CurrentUser User user) {
        try {
            Playlist createdPlaylist = playlistService.createPlaylist(
                playlist.getName(), 
                playlist.getDescription(), 
                user
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(new PlaylistResponse(createdPlaylist));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @SecuredPlaylistResource(optionalAuth = true)
    @Loggable(level = LogLevel.DEBUG, entryMessage = "Get playlist", includeResult = true)
    public ResponseEntity<Playlist> getPlaylist(
            @PathVariable("id") String id, 
            @CurrentUser(required = false) User user,
            @ResolvedResource Playlist playlist) {
        
        return ResponseEntity.ok(playlist);
    }

    @GetMapping("/{id}/videos")
    @SecuredPlaylistResource(optionalAuth = true)
    @Loggable(entryMessage = "Get playlist videos", includeParameters = true)
    public ResponseEntity<List<Video>> getPlaylistVideos(
            @PathVariable("id") String id, 
            @CurrentUser(required = false) User user,
            @ResolvedResource Playlist playlist) {
        
        try {
            List<Video> videos = playlistService.getPlaylistVideos(id);
            return ResponseEntity.ok(videos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my-playlists")
    @UserAction
    @Loggable(entryMessage = "Get user playlists", includeParameters = true)
    public ResponseEntity<List<Playlist>> getMyPlaylists(@CurrentUser User user) {
        List<Playlist> playlists = playlistService.listPlaylists(user.getId());
        return ResponseEntity.ok(playlists);
    }

    @PutMapping("/{id}")
    @SecuredPlaylistResource(requireWrite = true)
    @Loggable(entryMessage = "Update playlist", includeResult = true)
    public ResponseEntity<Playlist> updatePlaylist(
            @PathVariable("id") String id, 
            @RequestBody Playlist playlistDetails, 
            @CurrentUser User user,
            @ResolvedResource Playlist playlist) {
        
        try {
            // Update the playlist with new details
            playlist.setName(playlistDetails.getName());
            playlist.setDescription(playlistDetails.getDescription());
            
            // Creating a new playlist with the same ID to replace the old one
            Playlist updatedPlaylist = playlistService.createPlaylist(
                    playlist.getName(),
                    playlist.getDescription(),
                    user
            );
            
            return ResponseEntity.ok(updatedPlaylist);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{playlistId}/videos/{videoId}")
    @SecuredPlaylistResource(requireWrite = true, idParameter = "playlistId")
    @Loggable(entryMessage = "Add video to playlist", includeParameters = true)
    public ResponseEntity<?> addVideoToPlaylist(
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
            
            playlistService.addVideoToPlaylist(playlistId, videoId);
            
            if (details != null) {
                Video video = videoOpt.get();
                if (details.getSeasonNumber() != null) {
                    video.setSeasonNumber(details.getSeasonNumber());
                }
                if (details.getEpisodeNumber() != null) {
                    video.setEpisodeNumber(details.getEpisodeNumber());
                }
                videoService.updateVideo(video);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding video to playlist: " + e.getMessage());
        }
    }

    @DeleteMapping("/{playlistId}/videos/{videoId}")
    @SecuredPlaylistResource(requireWrite = true, idParameter = "playlistId")
    @Loggable(entryMessage = "Remove video from playlist", includeParameters = true)
    public ResponseEntity<?> removeVideoFromPlaylist(
            @PathVariable("playlistId") String playlistId, 
            @PathVariable("videoId") String videoId,
            @CurrentUser User user) {
        
        try {
            playlistService.removeVideoFromPlaylist(playlistId, videoId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
