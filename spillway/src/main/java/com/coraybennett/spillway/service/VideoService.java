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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
public class VideoService {
    private final VideoConversionService videoConversionService;
    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${video.upload-temp-dir:temp/uploads}")
    private String tempUploadDir;

    @Autowired
    public VideoService(VideoConversionService videoConversionService, 
                       VideoRepository videoRepository, 
                       PlaylistRepository playlistRepository) {
        this.videoConversionService = videoConversionService;
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
    }

    @Transactional
    public VideoResponse createVideo(VideoUploadRequest metadata) {
        Video video = new Video();
        video.setTitle(metadata.getTitle());
        video.setType(metadata.getType());
        video.setLength(metadata.getLength());
        video.setGenre(metadata.getGenre());
        video.setDescription(metadata.getDescription());
        video.setSeasonNumber(metadata.getSeasonNumber());
        video.setEpisodeNumber(metadata.getEpisodeNumber());
        video.setConversionStatus(ConversionStatus.PENDING);
        
        // Set placeholder URL that will be updated after conversion
        video.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, "pending"));
        
        // Add to playlist if provided
        if (metadata.getPlaylistId() != null) {
            Optional<Playlist> playlist = playlistRepository.findById(metadata.getPlaylistId());
            playlist.ifPresent(video::setPlaylist);
        }
        
        // Save video to get the ID
        Video savedVideo = videoRepository.save(video);
        
        // Update with actual URL
        savedVideo.setPlaylistUrl(String.format("%s/video/%s/playlist", baseUrl, savedVideo.getId()));
        savedVideo = videoRepository.save(savedVideo);
        
        return new VideoResponse(savedVideo);
    }

    @Transactional
    public void uploadAndConvertVideo(String videoId, MultipartFile videoFile) 
            throws VideoConversionException {
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoConversionException("Video not found: " + videoId));
        
        // Save the file to a temporary location immediately
        Path tempFile = null;
        try {
            // Create temp directory if it doesn't exist
            Path tempDir = Paths.get(tempUploadDir);
            Files.createDirectories(tempDir);
            
            // Create a temporary file to store the upload
            String tempFileName = videoId + "_" + sanitizeFilename(videoFile.getOriginalFilename());
            tempFile = tempDir.resolve(tempFileName);
            
            // Copy the uploaded file to the temporary location
            Files.copy(videoFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Start async conversion with the temporary file path
            videoConversionService.convertToHls(tempFile, video);
            
        } catch (IOException e) {
            // Clean up temp file if it was created
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ex) {
                    System.out.println("Failed to delete temp file after error" + ex.getMessage());
                }
            }
            throw new VideoConversionException("Failed to save uploaded file", e);
        }
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

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "upload.mp4";
        }
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    public static record ConversionProgress(
        ConversionStatus status,
        Integer progress,
        String error
    ) {}
}