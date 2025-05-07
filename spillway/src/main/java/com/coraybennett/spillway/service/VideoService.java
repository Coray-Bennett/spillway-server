package com.coraybennett.spillway.service;

import com.coraybennett.spillway.dto.VideoResponse;
import com.coraybennett.spillway.dto.VideoUploadRequest;
import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.PlaylistRepository;
import com.coraybennett.spillway.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class VideoService {
    private final VideoConversionService videoConversionService;
    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    @Autowired
    public VideoService(VideoConversionService videoConversionService, 
                       VideoRepository videoRepository, 
                       PlaylistRepository playlistRepository) {
        this.videoConversionService = videoConversionService;
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
    }

    @Transactional
    public VideoResponse uploadAndConvertVideo(MultipartFile videoFile, VideoUploadRequest metadata) 
            throws VideoConversionException {
        
        // Convert video to HLS
        String tag = videoConversionService.convertToHls(videoFile);
        
        // Create video entity
        Video video = new Video();
        video.setTitle(metadata.getTitle());
        video.setTag(tag);
        video.setType(metadata.getType());
        video.setLength(metadata.getLength());
        video.setGenre(metadata.getGenre());
        video.setDescription(metadata.getDescription());
        video.setSeasonNumber(metadata.getSeasonNumber());
        video.setEpisodeNumber(metadata.getEpisodeNumber());
        
        // Construct playlist URL
        String playlistUrl = String.format("%s/video/%s.m3u8", baseUrl, tag);
        video.setPlaylistUrl(playlistUrl);
        
        // Add to playlist if provided
        if (metadata.getPlaylistId() != null) {
            Optional<Playlist> playlist = playlistRepository.findById(metadata.getPlaylistId());
            playlist.ifPresent(video::setPlaylist);
        }
        
        Video savedVideo = videoRepository.save(video);
        return new VideoResponse(savedVideo);
    }

    public Optional<Video> getVideoById(String id) {
        return videoRepository.findById(id);
    }

    public Optional<Video> getVideoByTag(String tag) {
        return videoRepository.findByTag(tag);
    }
}