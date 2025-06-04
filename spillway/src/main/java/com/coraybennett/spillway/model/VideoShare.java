package com.coraybennett.spillway.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Entity representing a video share relationship between users.
 * Allows video owners to share their videos with specific users.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "video_shares")
@Slf4j
public class VideoShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
    
    @ManyToOne
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedBy;
    
    @ManyToOne
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWith;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePermission permission = SharePermission.READ;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        log.debug("Creating video share: video {} shared by {} with {}", 
                 video.getId(), sharedBy.getUsername(), sharedWith.getUsername());
    }
    
    public VideoShare(Video video, User sharedBy, User sharedWith, SharePermission permission) {
        this.video = video;
        this.sharedBy = sharedBy;
        this.sharedWith = sharedWith;
        this.permission = permission;
    }
    
    public VideoShare(Video video, User sharedBy, User sharedWith, SharePermission permission, LocalDateTime expiresAt) {
        this(video, sharedBy, sharedWith, permission);
        this.expiresAt = expiresAt;
    }
    
    /**
     * Checks if this share is currently valid (active and not expired).
     */
    public boolean isValid() {
        return active && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
    
    public enum SharePermission {
        READ, // Can view the video
        MODIFY, // Can edit video metadata (future extension)
        ADMIN // Can share with others (future extension)
    }
}