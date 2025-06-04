package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;
import com.coraybennett.spillway.model.VideoType;
import com.coraybennett.spillway.model.ConversionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
    
    public void setSize(Integer size) { 
        this.size = Math.min(size, 100); // Cap at 100
    }
}