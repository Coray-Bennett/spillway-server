package com.coraybennett.spillway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for video update requests.
 * Only includes fields that can be updated by the user.
 */
@Data
@NoArgsConstructor
public class VideoUpdateRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @Size(max = 100, message = "Genre cannot exceed 100 characters")
    private String genre;
    
    // For episode videos
    private Integer seasonNumber;
    private Integer episodeNumber;
}