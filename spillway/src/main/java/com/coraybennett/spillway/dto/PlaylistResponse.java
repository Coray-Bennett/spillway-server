package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;
import com.coraybennett.spillway.model.Playlist;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
}