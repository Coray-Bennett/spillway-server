package com.coraybennett.spillway.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.PlaylistRepository;
import com.coraybennett.spillway.repository.VideoRepository;

@RestController
@RequestMapping("/playlist")
public class PlaylistController {
    private final PlaylistRepository playlistRepository;
    private final VideoRepository videoRepository;

    @Autowired
    public PlaylistController(PlaylistRepository playlistRepository, VideoRepository videoRepository) {
        this.playlistRepository = playlistRepository;
        this.videoRepository = videoRepository;
    }

    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(@RequestBody Playlist playlist) {
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
    public ResponseEntity<List<Playlist>> getMyPlaylists() {
        // For now, this returns all playlists
        List<Playlist> playlists = playlistRepository.findAll();
        return ResponseEntity.ok(playlists);
    }
}