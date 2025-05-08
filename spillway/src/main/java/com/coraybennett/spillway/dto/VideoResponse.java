package com.coraybennett.spillway.dto;

import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoType;
import java.time.LocalDateTime;

public class VideoResponse {
    private String id;
    private String title;
    private String playlistUrl;
    private VideoType type;
    private Integer length;
    private String genre;
    private String description;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public VideoResponse(Video video) {
        this.id = video.getId();
        this.title = video.getTitle();
        this.playlistUrl = video.getPlaylistUrl();
        this.type = video.getType();
        this.length = video.getLength();
        this.genre = video.getGenre();
        this.description = video.getDescription();
        this.seasonNumber = video.getSeasonNumber();
        this.episodeNumber = video.getEpisodeNumber();
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