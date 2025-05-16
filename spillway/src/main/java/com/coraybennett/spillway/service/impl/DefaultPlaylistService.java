package com.coraybennett.spillway.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.PlaylistRepository;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.PlaylistService;

/**
 * Default implementation of PlaylistService.
 */
@Service
public class DefaultPlaylistService implements PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final VideoRepository videoRepository;

    @Autowired
    public DefaultPlaylistService(PlaylistRepository playlistRepository, VideoRepository videoRepository) {
        this.playlistRepository = playlistRepository;
        this.videoRepository = videoRepository;
    }

    @Override
    @Transactional
    public Playlist createPlaylist(String name, String description, User user) {
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setCreatedBy(user);
        playlist.setVideos(new ArrayList<>());
        return playlistRepository.save(playlist);
    }

    @Override
    public Optional<Playlist> getPlaylistById(String id) {
        return playlistRepository.findById(id);
    }

    @Override
    public List<Playlist> listPlaylists(String userId) {
        if (userId != null && !userId.isEmpty()) {
            return playlistRepository.findByCreatedById(userId);
        } else {
            return playlistRepository.findAll();
        }
    }

    @Override
    @Transactional
    public Playlist addVideoToPlaylist(String playlistId, String videoId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));
        
        // Avoid duplicates
        if (!playlist.getVideos().contains(video)) {
            playlist.getVideos().add(video);
            video.setPlaylist(playlist);
            videoRepository.save(video);
        }
        
        return playlistRepository.save(playlist);
    }

    @Override
    @Transactional
    public Playlist removeVideoFromPlaylist(String playlistId, String videoId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));
        
        if (playlist.getVideos().contains(video)) {
            playlist.getVideos().remove(video);
            video.setPlaylist(null);
            videoRepository.save(video);
        }
        
        return playlistRepository.save(playlist);
    }

    @Override
    public List<Video> getPlaylistVideos(String playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
        
        return playlist.getVideos();
    }
}