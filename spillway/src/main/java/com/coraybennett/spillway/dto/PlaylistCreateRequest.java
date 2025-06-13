package com.coraybennett.spillway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for playlist creation requests.
 */
@Data
@NoArgsConstructor
public class PlaylistCreateRequest {
    
    @NotBlank(message = "Playlist name is required")
    @Size(min = 1, max = 255, message = "Playlist name must be between 1 and 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}