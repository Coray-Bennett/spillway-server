package com.coraybennett.spillway.service.impl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

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
import com.coraybennett.spillway.service.api.TemporaryKeyStorageService;
import com.coraybennett.spillway.service.api.VideoConversionService;
import com.coraybennett.spillway.service.api.VideoEncryptionService;
import com.coraybennett.spillway.service.api.VideoService;

import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of VideoService with encryption support.
 */
@Service
@Slf4j
public class DefaultVideoService implements VideoService {

    private final VideoConversionService videoConversionService;
    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    private final StorageService storageService;
    private final VideoEncryptionService videoEncryptionService;
    private final TemporaryKeyStorageService temporaryKeyStorage;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${video.upload-temp-dir:temp/uploads}")
    private String tempUploadDir;

    @Autowired
    public DefaultVideoService(
        VideoConversionService videoConversionService, 
        VideoRepository videoRepository, 
        PlaylistRepository playlistRepository,
        StorageService storageService,
        VideoEncryptionService videoEncryptionService,
        TemporaryKeyStorageService temporaryKeyStorage
    ) {
        this.videoConversionService = videoConversionService;
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
        this.storageService = storageService;
        this.videoEncryptionService = videoEncryptionService;
        this.temporaryKeyStorage = temporaryKeyStorage;
    }

    @Override
    @Transactional
    public VideoResponse createVideo(VideoUploadRequest metadata, User user) {
        log.info("Creating video '{}' for user '{}' (encrypted: {})", 
            metadata.getTitle(), user.getUsername(), metadata.isEncrypted());
        
        Video video = new Video();
        video.setTitle(metadata.getTitle());
        video.setType(metadata.getType());
        video.setLength(metadata.getLength() != null ? metadata.getLength() : 0);
        video.setGenre(metadata.getGenre());
        video.setDescription(metadata.getDescription());
        video.setSeasonNumber(metadata.getSeasonNumber());
        video.setEpisodeNumber(metadata.getEpisodeNumber());
        video.setConversionStatus(ConversionStatus.PENDING);
        video.setUploadedBy(user);
        
        // Handle encryption settings
        if (metadata.isEncrypted()) {
            if (!videoEncryptionService.isValidKey(metadata.getEncryptionKey())) {
                throw new IllegalArgumentException("Invalid encryption key provided");
            }
            
            video.setEncrypted(true);
            // Store a hash of the key for validation purposes (never store the actual key)
            video.setEncryptionKeyHash(hashEncryptionKey(metadata.getEncryptionKey()));
            log.info("Video will be encrypted during processing");
        }
        
        video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, "pending"));
        
        if (metadata.getPlaylistId() != null) {
            log.debug("Associating video with playlist ID: {}", metadata.getPlaylistId());
            Optional<Playlist> playlist = playlistRepository.findById(metadata.getPlaylistId());
            playlist.ifPresent(video::setPlaylist);
        }
        
        Video savedVideo = videoRepository.save(video);
        savedVideo.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, savedVideo.getId()));
        savedVideo = videoRepository.save(savedVideo);
        
        // Encryption key is stored by the controller, no need to store here
        
        log.info("Successfully created video with ID: {} for user: {}", 
                 savedVideo.getId(), user.getUsername());
        return new VideoResponse(savedVideo);
    }

    @Override
    @Transactional
    public void uploadAndConvertVideo(String videoId, MultipartFile videoFile) 
            throws VideoConversionException {
        log.info("Starting video upload and conversion for video ID: {}", videoId);
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoConversionException("Video not found: " + videoId));
        
        try {
            Path tempFilePath = storageService.store(videoFile, tempUploadDir);
            log.debug("Video file temporarily stored at: {}", tempFilePath);
            
            video.setConversionStatus(ConversionStatus.IN_PROGRESS);
            videoRepository.save(video);
            
            // Pass encryption key if video is encrypted
            String encryptionKey = null;
            if (video.isEncrypted()) {
                encryptionKey = temporaryKeyStorage.retrieveKey(videoId);
                if (encryptionKey == null) {
                    throw new VideoConversionException("Encryption key not found for encrypted video");
                }
            }
            
            // Modified conversion call to support encryption
            if (videoConversionService instanceof EncryptedVideoConversionService) {
                ((EncryptedVideoConversionService) videoConversionService)
                    .convertToHls(tempFilePath, video, encryptionKey);
            } else {
                videoConversionService.convertToHls(tempFilePath, video);
            }
            
            log.info("Video conversion initiated for video ID: {}", videoId);
            
        } catch (Exception e) {
            log.error("Failed to upload/convert video {}: {}", videoId, e.getMessage(), e);
            video.setConversionStatus(ConversionStatus.FAILED);
            video.setConversionError(e.getMessage());
            videoRepository.save(video);
            throw new VideoConversionException("Video conversion failed: " + e.getMessage(), e);
        } finally {
            // Clean up temporary encryption key after conversion starts
            if (video.isEncrypted()) {
                temporaryKeyStorage.deleteKey(videoId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Video> getVideoById(String id) {
        log.debug("Fetching video by ID: {}", id);
        return videoRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversionProgress getConversionProgress(String id) {
        log.debug("Checking conversion progress for video ID: {}", id);
        
        Optional<Video> videoOpt = videoRepository.findById(id);
        if (videoOpt.isEmpty()) {
            log.warn("Video not found for progress check: {}", id);
            return null;
        }
        
        Video video = videoOpt.get();
        return new ConversionProgress(
            video.getConversionStatus(), 
            video.getConversionProgress(), 
            video.getConversionError()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> listVideos(String userId) {
        log.debug("Listing videos for user ID: {}", userId);
        
        if (userId != null) {
            List<Video> userVideos = videoRepository.findByUploadedById(userId);
            log.debug("Found {} videos for user {}", userVideos.size(), userId);
            return userVideos;
        }
        
        List<Video> allVideos = videoRepository.findAll();
        log.debug("Found {} total videos", allVideos.size());
        return allVideos;
    }

    @Override
    @Transactional
    public Video updateVideo(Video video) {
        log.info("Updating video: {}", video.getId());
        return videoRepository.save(video);
    }

    @Override
    public VideoConversionService getVideoConversionService() {
        return videoConversionService;
    }
    
    /**
     * Hashes an encryption key using SHA-256 for storage.
     * We never store the actual encryption key in the database.
     */
    private String hashEncryptionKey(String encryptionKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash encryption key", e);
        }
    }
}