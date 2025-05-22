package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;
import com.coraybennett.spillway.model.VideoType;
import com.coraybennett.spillway.model.ConversionStatus;

public class VideoSearchRequest {
    private String query; // General search query for title/description
    private String title;
    private String genre;
    private VideoType type;
    private ConversionStatus conversionStatus;
    private Integer minLength; // in seconds
    private Integer maxLength; // in seconds
    private LocalDateTime uploadedAfter;
    private LocalDateTime uploadedBefore;
    private String uploadedBy; // username
    private String playlistId;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private String sortBy; // title, uploadDate, length
    private String sortDirection; // ASC, DESC
    private Integer page = 0;
    private Integer size = 20;
    
    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public VideoType getType() { return type; }
    public void setType(VideoType type) { this.type = type; }
    
    public ConversionStatus getConversionStatus() { return conversionStatus; }
    public void setConversionStatus(ConversionStatus conversionStatus) { this.conversionStatus = conversionStatus; }
    
    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }
    
    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
    
    public LocalDateTime getUploadedAfter() { return uploadedAfter; }
    public void setUploadedAfter(LocalDateTime uploadedAfter) { this.uploadedAfter = uploadedAfter; }
    
    public LocalDateTime getUploadedBefore() { return uploadedBefore; }
    public void setUploadedBefore(LocalDateTime uploadedBefore) { this.uploadedBefore = uploadedBefore; }
    
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    
    public String getPlaylistId() { return playlistId; }
    public void setPlaylistId(String playlistId) { this.playlistId = playlistId; }
    
    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
    
    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = Math.min(size, 100); } // Cap at 100
}