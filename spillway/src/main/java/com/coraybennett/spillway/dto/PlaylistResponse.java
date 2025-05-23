package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;
import com.coraybennett.spillway.model.Playlist;

public class PlaylistResponse {
    private String id;
    private String name;
    private String description;
    private int videoCount;
    private String createdByUsername;
    private String createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalDuration; // Sum of all video lengths in seconds
    
    public PlaylistResponse(Playlist playlist) {
        this.id = playlist.getId();
        this.name = playlist.getName();
        this.description = playlist.getDescription();
        this.videoCount = playlist.getVideos() != null ? playlist.getVideos().size() : 0;
        this.createdByUsername = playlist.getCreatedBy() != null ? playlist.getCreatedBy().getUsername() : null;
        this.createdById = playlist.getCreatedBy() != null ? playlist.getCreatedBy().getId() : null;
        this.createdAt = playlist.getCreatedAt();
        this.updatedAt = playlist.getUpdatedAt();
        
        // Calculate total duration
        if (playlist.getVideos() != null) {
            this.totalDuration = playlist.getVideos().stream()
                .mapToInt(v -> v.getLength() != null ? v.getLength() : 0)
                .sum();
        } else {
            this.totalDuration = 0;
        }
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getVideoCount() { return videoCount; }
    public void setVideoCount(int videoCount) { this.videoCount = videoCount; }
    
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    
    public String getCreatedById() { return createdById; }
    public void setCreatedById(String createdById) { this.createdById = createdById; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getTotalDuration() { return totalDuration; }
    public void setTotalDuration(Integer totalDuration) { this.totalDuration = totalDuration; }
}