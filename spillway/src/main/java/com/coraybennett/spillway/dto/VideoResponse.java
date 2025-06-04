package com.coraybennett.spillway.dto;

import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
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
}