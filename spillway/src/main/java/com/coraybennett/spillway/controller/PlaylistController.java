package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.Optional;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.dto.PlaylistVideoDetails;
import com.coraybennett.spillway.service.api.PlaylistService;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoService;

/**
 * Controller handling playlist operations.
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
    public ResponseEntity<Playlist> createPlaylist(@RequestBody Playlist playlist, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
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
    public ResponseEntity<Playlist> getPlaylist(@PathVariable String id, Principal principal) {
        Optional<Playlist> playlistOpt = playlistService.getPlaylistById(id);
        
        if (playlistOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Playlist playlist = playlistOpt.get();
        
        // Check access permission
        User user = principal != null ? 
            userService.findByUsername(principal.getName()).orElse(null) : null;
        
        if (!videoAccessService.canAccessPlaylist(playlist, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(playlist);
    }

    @GetMapping("/{id}/videos")
    public ResponseEntity<List<Video>> getPlaylistVideos(@PathVariable String id, Principal principal) {
        try {
            Optional<Playlist> playlistOpt = playlistService.getPlaylistById(id);
            
            if (playlistOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Playlist playlist = playlistOpt.get();
            
            // Check access permission
            User user = principal != null ? 
                userService.findByUsername(principal.getName()).orElse(null) : null;
            
            if (!videoAccessService.canAccessPlaylist(playlist, user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<Video> videos = playlistService.getPlaylistVideos(id);
            return ResponseEntity.ok(videos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my-playlists")
    public ResponseEntity<List<Playlist>> getMyPlaylists(Principal principal) {
        User user = userService.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        List<Playlist> playlists = playlistService.listPlaylists(user.getId());
        return ResponseEntity.ok(playlists);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Playlist> updatePlaylist(
            @PathVariable String id, 
            @RequestBody Playlist playlistDetails, 
            Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<Playlist> playlistOpt = playlistService.getPlaylistById(id);
            if (playlistOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Playlist playlist = playlistOpt.get();
            
            if (!playlist.getCreatedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
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
    public ResponseEntity<?> addVideoToPlaylist(
            @PathVariable String playlistId, 
            @PathVariable String videoId,
            @RequestBody(required = false) PlaylistVideoDetails details,
            Principal principal) {
        
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<Playlist> playlistOpt = playlistService.getPlaylistById(playlistId);
            Optional<Video> videoOpt = videoService.getVideoById(videoId);
            
            if (playlistOpt.isEmpty() || videoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Playlist playlist = playlistOpt.get();
            
            if (!playlist.getCreatedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
    public ResponseEntity<?> removeVideoFromPlaylist(
            @PathVariable String playlistId, 
            @PathVariable String videoId,
            Principal principal) {
        
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<Playlist> playlistOpt = playlistService.getPlaylistById(playlistId);
            
            if (playlistOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Playlist playlist = playlistOpt.get();
            
            if (!playlist.getCreatedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            playlistService.removeVideoFromPlaylist(playlistId, videoId);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
