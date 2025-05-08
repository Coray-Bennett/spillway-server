package com.coraybennett.spillway.dto;

import com.coraybennett.spillway.model.VideoType;

public class VideoUploadRequest {
    private String title;
    private VideoType type;
    private Integer length;
    private String genre;
    private String description;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private String playlistId;

    public VideoUploadRequest(String title, VideoType type, Integer length, String genre, String description,
            Integer seasonNumber, Integer episodeNumber, String playlistId) {
        this.title = title;
        this.type = type;
        this.length = length;
        this.genre = genre;
        this.description = description;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.playlistId = playlistId;
    }
    
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
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
    public String getPlaylistId() {
        return playlistId;
    }
    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }
}