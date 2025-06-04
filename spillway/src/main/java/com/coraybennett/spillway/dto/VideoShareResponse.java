package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;

import com.coraybennett.spillway.model.VideoShare;
import com.coraybennett.spillway.model.VideoShare.SharePermission;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for video share information.
 */
@Data
@NoArgsConstructor
public class VideoShareResponse {
    private String id;
    private String videoId;
    private String videoTitle;
    private String sharedByUsername;
    private String sharedWithUsername;
    private SharePermission permission;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
    private boolean isValid;
    
    public VideoShareResponse(VideoShare share) {
        this.id = share.getId();
        this.videoId = share.getVideo().getId();
        this.videoTitle = share.getVideo().getTitle();
        this.sharedByUsername = share.getSharedBy().getUsername();
        this.sharedWithUsername = share.getSharedWith().getUsername();
        this.permission = share.getPermission();
        this.createdAt = share.getCreatedAt();
        this.expiresAt = share.getExpiresAt();
        this.active = share.isActive();
        this.isValid = share.isValid();
    }
}