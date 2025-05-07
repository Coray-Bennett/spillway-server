package com.coraybennett.spillway.service;

import com.coraybennett.spillway.dto.VideoResponse;
import com.coraybennett.spillway.dto.VideoUploadRequest;
import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.ConversionStatus;
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
        
        // Create video entity
        Video video = new Video();
        video.setTitle(metadata.getTitle());
        video.setType(metadata.getType());
        video.setLength(metadata.getLength());
        video.setGenre(metadata.getGenre());
        video.setDescription(metadata.getDescription());
        video.setSeasonNumber(metadata.getSeasonNumber());
        video.setEpisodeNumber(metadata.getEpisodeNumber());
        video.setConversionStatus(ConversionStatus.PENDING);
        
        // Initially set a placeholder URL - will be updated after conversion
        video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, "pending"));
        
        // Add to playlist if provided
        if (metadata.getPlaylistId() != null) {
            Optional<Playlist> playlist = playlistRepository.findById(metadata.getPlaylistId());
            playlist.ifPresent(video::setPlaylist);
        }
        
        // Save video first to get the ID
        Video savedVideo = videoRepository.save(video);
        
        // Update playlist URL with actual ID
        savedVideo.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, savedVideo.getId()));
        savedVideo = videoRepository.save(savedVideo);
        
        // Start async conversion
        videoConversionService.convertToHls(videoFile, savedVideo);
        
        return new VideoResponse(savedVideo);
    }

    public Optional<Video> getVideoById(String id) {
        return videoRepository.findById(id);
    }
    
    public ConversionProgress getConversionProgress(String id) {
        Optional<Video> video = videoRepository.findById(id);
        if (video.isEmpty()) {
            return null;
        }
        
        Video v = video.get();
        return new ConversionProgress(
            v.getConversionStatus(),
            v.getConversionProgress(),
            v.getConversionError()
        );
    }
    
    public static record ConversionProgress(
        ConversionStatus status,
        Integer progress,
        String error
    ) {}
}