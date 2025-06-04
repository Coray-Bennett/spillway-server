package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;

import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VideoListResponse {
    private String id;
    private String title;
    private String playlistUrl;
    private VideoType type;
    private ConversionStatus conversionStatus;
    private Integer conversionProgress;
    private String conversionError;
    private Integer length;
    private String genre;
    private String description;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private String playlistName;
    private String uploaderUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public VideoListResponse(Video video) {
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
        this.playlistName = video.getPlaylist() != null ? video.getPlaylist().getName() : null;
        this.uploaderUsername = video.getUploadedBy() != null ? video.getUploadedBy().getUsername() : null;
        this.createdAt = video.getCreatedAt();
        this.updatedAt = video.getUpdatedAt();
    }
}