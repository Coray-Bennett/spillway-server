package com.coraybennett.spillway.service.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.dto.VideoResponse;
import com.coraybennett.spillway.dto.VideoUploadRequest;
import com.coraybennett.spillway.exception.VideoConversionException;
import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.repository.PlaylistRepository;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.VideoConversionService;
import com.coraybennett.spillway.service.api.VideoService;

/**
 * Default implementation of VideoService.
 */
@Service
public class DefaultVideoService implements VideoService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultVideoService.class);

    private final VideoConversionService videoConversionService;
    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    private final StorageService storageService;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${video.upload-temp-dir:temp/uploads}")
    private String tempUploadDir;

    @Autowired
    public DefaultVideoService(
        VideoConversionService videoConversionService, 
        VideoRepository videoRepository, 
        PlaylistRepository playlistRepository,
        StorageService storageService
    ) {
        this.videoConversionService = videoConversionService;
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public VideoResponse createVideo(VideoUploadRequest metadata, User user) {
        Video video = new Video();
        video.setTitle(metadata.getTitle());
        video.setType(metadata.getType());
        
        // Set a default length that will be updated after upload
        video.setLength(metadata.getLength() != null ? metadata.getLength() : 0);
        
        video.setGenre(metadata.getGenre());
        video.setDescription(metadata.getDescription());
        video.setSeasonNumber(metadata.getSeasonNumber());
        video.setEpisodeNumber(metadata.getEpisodeNumber());
        video.setConversionStatus(ConversionStatus.PENDING);
        video.setUploadedBy(user);
        
        video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, "pending"));
        
        if (metadata.getPlaylistId() != null) {
            Optional<Playlist> playlist = playlistRepository.findById(metadata.getPlaylistId());
            playlist.ifPresent(video::setPlaylist);
        }
        
        Video savedVideo = videoRepository.save(video);
        savedVideo.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, savedVideo.getId()));
        savedVideo = videoRepository.save(savedVideo);
        
        return new VideoResponse(savedVideo);
    }

    @Override
    @Transactional
    public void uploadAndConvertVideo(String videoId, MultipartFile videoFile) 
            throws VideoConversionException {
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoConversionException("Video not found: " + videoId));
        
        try {
            // Store the temporary file
            Path tempFilePath = storageService.store(
                    videoFile, 
                    tempUploadDir
            );
            
            // Update video duration based on the actual file
            int duration = videoConversionService.getVideoDuration(tempFilePath);
            if (duration > 0) {
                video.setLength(duration);
                videoRepository.save(video);
                logger.info("Updated video duration to {} seconds for video {}", duration, videoId);
            }
            
            // Start the conversion process
            videoConversionService.convertToHls(tempFilePath, video);
            
        } catch (Exception e) {
            throw new VideoConversionException("Failed to process uploaded file", e);
        }
    }

    @Override
    public Optional<Video> getVideoById(String id) {
        return videoRepository.findById(id);
    }
    
    @Override
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
    
    @Override
    public List<Video> listVideos(String userId) {
        if (userId != null && !userId.isEmpty()) {
            return videoRepository.findByUploadedById(userId);
        } else {
            return videoRepository.findAll();
        }
    }
    
    @Override
    public VideoConversionService getVideoConversionService() {
        return videoConversionService;
    }
    
    @Override
    @Transactional
    public Video updateVideo(Video video) {
        if (!videoRepository.existsById(video.getId())) {
            throw new IllegalArgumentException("Cannot update non-existent video: " + video.getId());
        }
        return videoRepository.save(video);
    }
}