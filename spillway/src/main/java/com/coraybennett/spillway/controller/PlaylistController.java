package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.RequiresAuthentication;
import com.coraybennett.spillway.annotation.ResourceAccess;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.dto.PlaylistVideoDetails;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.PlaylistService;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoService;

/**
 * Refactored controller handling playlist operations using annotations for access control.
 */
@RestController
@RequestMapping("/playlist")
public class PlaylistController {
    private final PlaylistService playlistService;
    private final VideoService videoService;
    private final UserService userService;
    private final VideoAccessService videoAccessService;

    @Autowired
    public PlaylistController(
            PlaylistService playlistService, 
            VideoService videoService, 
            UserService userService,
            VideoAccessService videoAccessService
        ) {
        this.playlistService = playlistService;
        this.videoService = videoService;
        this.userService = userService;
        this.videoAccessService = videoAccessService;
    }

    @PostMapping
    @RequiresAuthentication
    public ResponseEntity<Playlist> createPlaylist(@RequestBody Playlist playlist, @CurrentUser User user) {
        try {
            Playlist createdPlaylist = playlistService.createPlaylist(
                playlist.getName(), 
                playlist.getDescription(), 
                user
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaylist);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @ResourceAccess(
        resourceType = ResourceAccess.ResourceType.PLAYLIST,
        handling = ResourceAccess.ResourceHandling.INJECT_RESOLVED
    )
    public ResponseEntity<Playlist> getPlaylist(
            @PathVariable String id, 
            @CurrentUser(required = false) User user,
            @ResolvedResource Playlist playlist) {
        
        return ResponseEntity.ok(playlist);
    }

    @GetMapping("/{id}/videos")
    @ResourceAccess(
        resourceType = ResourceAccess.ResourceType.PLAYLIST,
        handling = ResourceAccess.ResourceHandling.INJECT_RESOLVED
    )
    public ResponseEntity<List<Video>> getPlaylistVideos(
            @PathVariable String id, 
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
    @RequiresAuthentication
    public ResponseEntity<List<Playlist>> getMyPlaylists(@CurrentUser User user) {
        List<Playlist> playlists = playlistService.listPlaylists(user.getId());
        return ResponseEntity.ok(playlists);
    }

    @PutMapping("/{id}")
    @RequiresAuthentication
    @ResourceAccess(
        resourceType = ResourceAccess.ResourceType.PLAYLIST,
        requireWriteAccess = true,
        handling = ResourceAccess.ResourceHandling.INJECT_RESOLVED
    )
    public ResponseEntity<Playlist> updatePlaylist(
            @PathVariable String id, 
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
    @RequiresAuthentication
    @ResourceAccess(
        resourceType = ResourceAccess.ResourceType.PLAYLIST,
        idParameter = "playlistId",
        requireWriteAccess = true,
        handling = ResourceAccess.ResourceHandling.INJECT_RESOLVED
    )
    public ResponseEntity<?> addVideoToPlaylist(
            @PathVariable String playlistId, 
            @PathVariable String videoId,
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
    @RequiresAuthentication
    @ResourceAccess(
        resourceType = ResourceAccess.ResourceType.PLAYLIST,
        idParameter = "playlistId",
        requireWriteAccess = true
    )
    public ResponseEntity<?> removeVideoFromPlaylist(
            @PathVariable String playlistId, 
            @PathVariable String videoId,
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