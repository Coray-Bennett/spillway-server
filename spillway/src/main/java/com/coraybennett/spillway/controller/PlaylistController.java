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
import com.coraybennett.spillway.repository.PlaylistRepository;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.repository.UserRepository;
import com.coraybennett.spillway.dto.PlaylistVideoDetails;

@RestController
@RequestMapping("/playlist")
public class PlaylistController {
    private final PlaylistRepository playlistRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Autowired
    public PlaylistController(PlaylistRepository playlistRepository, VideoRepository videoRepository, UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(@RequestBody Playlist playlist, Principal principal) {
        // Get current user
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Set the creator
        playlist.setCreatedBy(user);
        
        Playlist savedPlaylist = playlistRepository.save(playlist);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPlaylist);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Playlist> getPlaylist(@PathVariable String id) {
        Optional<Playlist> playlist = playlistRepository.findById(id);
        return playlist.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/videos")
    public ResponseEntity<List<Video>> getPlaylistVideos(@PathVariable String id) {
        List<Video> videos = videoRepository.findByPlaylistIdOrderBySeasonNumberAscEpisodeNumberAsc(id);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/my-playlists")
    public ResponseEntity<List<Playlist>> getMyPlaylists(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        List<Playlist> playlists = playlistRepository.findByCreatedBy(user);
        return ResponseEntity.ok(playlists);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Playlist> updatePlaylist(@PathVariable String id, @RequestBody Playlist playlistDetails, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<Playlist> playlistOpt = playlistRepository.findById(id);
        if (playlistOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Playlist playlist = playlistOpt.get();
        
        // Check if user has permission (is the creator)
        if (!playlist.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Update playlist properties
        playlist.setName(playlistDetails.getName());
        playlist.setDescription(playlistDetails.getDescription());
        
        Playlist updatedPlaylist = playlistRepository.save(playlist);
        return ResponseEntity.ok(updatedPlaylist);
    }

    @PostMapping("/{playlistId}/videos/{videoId}")
    public ResponseEntity<?> addVideoToPlaylist(
            @PathVariable String playlistId, 
            @PathVariable String videoId,
            @RequestBody(required = false) PlaylistVideoDetails details,
            Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        
        if (playlistOpt.isEmpty() || videoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Playlist playlist = playlistOpt.get();
        Video video = videoOpt.get();
        
        // Check if user has permission
        if (!playlist.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Update video with playlist relationship
        video.setPlaylist(playlist);
        
        // If details are provided, update episode/season info
        if (details != null) {
            if (details.getSeasonNumber() != null) {
                video.setSeasonNumber(details.getSeasonNumber());
            }
            if (details.getEpisodeNumber() != null) {
                video.setEpisodeNumber(details.getEpisodeNumber());
            }
        }
        
        videoRepository.save(video);
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{playlistId}/videos/{videoId}")
    public ResponseEntity<?> removeVideoFromPlaylist(
            @PathVariable String playlistId, 
            @PathVariable String videoId,
            Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        
        if (playlistOpt.isEmpty() || videoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Playlist playlist = playlistOpt.get();
        Video video = videoOpt.get();
        
        // Check if user has permission
        if (!playlist.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Check if video is actually in this playlist
        if (!playlist.equals(video.getPlaylist())) {
            return ResponseEntity.badRequest().body("Video is not in this playlist");
        }
        
        // Remove video from playlist
        video.setPlaylist(null);
        video.setSeasonNumber(null);
        video.setEpisodeNumber(null);
        
        videoRepository.save(video);
        
        return ResponseEntity.ok().build();
    }
}