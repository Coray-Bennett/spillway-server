package com.coraybennett.spillway.dto;

import com.coraybennett.spillway.model.Video;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for video update response.
 * Returns the updated video information after a successful update.
 */
@Data
@NoArgsConstructor
public class VideoUpdateResponse {
    private String id;
    private String title;
    private String description;
    private String genre;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private LocalDateTime updatedAt;
    
    public VideoUpdateResponse(Video video) {
        this.id = video.getId();
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.genre = video.getGenre();
        this.seasonNumber = video.getSeasonNumber();
        this.episodeNumber = video.getEpisodeNumber();
        this.updatedAt = video.getUpdatedAt();
    }
}