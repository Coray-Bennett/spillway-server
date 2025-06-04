package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;

import com.coraybennett.spillway.model.VideoShare.SharePermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sharing a video with another user.
 */
@Data
@NoArgsConstructor
public class VideoShareRequest {
    
    @NotNull(message = "Video ID is required")
    @NotBlank(message = "Video ID cannot be blank")
    private String videoId;
    
    @NotNull(message = "Username of user to share with is required")
    @NotBlank(message = "Username cannot be blank")
    private String sharedWithUsername;
    
    @NotNull(message = "Permission level is required")
    private SharePermission permission = SharePermission.READ;
    
    private LocalDateTime expiresAt;
}