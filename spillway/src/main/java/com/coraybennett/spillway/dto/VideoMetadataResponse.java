package com.coraybennett.spillway.dto;

import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning complete video metadata including ownership information.
 * Used by the video metadata endpoint to provide all necessary information
 * for the frontend to render video details and determine ownership.
 */
@Data
@NoArgsConstructor
public class VideoMetadataResponse {
    private String id;
    private String title;
    private String playlistUrl;
    private VideoType type;
    private ConversionStatus conversionStatus;
    private Integer conversionProgress;
    private String conversionError;
    private Integer length; // in seconds
    private String genre;
    private String description;
    private Integer seasonNumber;
    private Integer episodeNumber;
    
    // Ownership information
    private UploadedByInfo uploadedBy;
    
    // Playlist information
    private String playlistId;
    private String playlistName;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Nested DTO for uploader information
     */
    @Data
    @NoArgsConstructor
    public static class UploadedByInfo {
        private String id;
        private String username;
        
        public UploadedByInfo(String id, String username) {
            this.id = id;
            this.username = username;
        }
    }
    
    /**
     * Constructor to create response from Video entity
     * @param video The video entity
     */
    public VideoMetadataResponse(Video video) {
        this.id = video.getId();
        this.title = video.getTitle();
        this.playlistUrl = video.getPlaylistUrl();
        this.type = video.getType();
        this.conversionStatus = video.getConversionStatus();
        this.conversionProgress = video.getConversionProgress();
        this.conversionError = video.getConversionError();
        this.length = video.getLength();
        this.genre = video.getGenre();
        this.description = video.getDescription();
        this.seasonNumber = video.getSeasonNumber();
        this.episodeNumber = video.getEpisodeNumber();
        this.createdAt = video.getCreatedAt();
        this.updatedAt = video.getUpdatedAt();
        
        // Set ownership information
        if (video.getUploadedBy() != null) {
            this.uploadedBy = new UploadedByInfo(
                video.getUploadedBy().getId(),
                video.getUploadedBy().getUsername()
            );
        }
        
        // Set playlist information if available
        if (video.getPlaylist() != null) {
            this.playlistId = video.getPlaylist().getId();
            this.playlistName = video.getPlaylist().getName();
        }
    }
}