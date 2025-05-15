package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;

import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoType;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlaylistUrl() {
        return playlistUrl;
    }

    public void setPlaylistUrl(String playlistUrl) {
        this.playlistUrl = playlistUrl;
    }

    public VideoType getType() {
        return type;
    }

    public void setType(VideoType type) {
        this.type = type;
    }

    public ConversionStatus getConversionStatus() {
        return conversionStatus;
    }

    public void setConversionStatus(ConversionStatus conversionStatus) {
        this.conversionStatus = conversionStatus;
    }

    public Integer getConversionProgress() {
        return conversionProgress;
    }

    public void setConversionProgress(Integer conversionProgress) {
        this.conversionProgress = conversionProgress;
    }

    public String getConversionError() {
        return conversionError;
    }

    public void setConversionError(String conversionError) {
        this.conversionError = conversionError;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public String getUploaderUsername() {
        return uploaderUsername;
    }

    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}