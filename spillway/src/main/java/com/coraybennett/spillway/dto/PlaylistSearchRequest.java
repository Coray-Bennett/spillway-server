package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlaylistSearchRequest {
    private String query; // General search query for name/description
    private String name;
    private String description;
    private String createdBy; // username
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private Integer minVideoCount;
    private Integer maxVideoCount;
    private String sortBy; // name, createdDate, videoCount
    private String sortDirection; // ASC, DESC
    private Integer page = 0;
    private Integer size = 20;
    
    public void setSize(Integer size) { 
        this.size = Math.min(size, 100); // Cap at 100
    }
}