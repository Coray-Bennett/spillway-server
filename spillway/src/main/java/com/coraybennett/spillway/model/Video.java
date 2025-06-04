package com.coraybennett.spillway.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Entity
@Table(name = "videos")
@Slf4j
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String playlistUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversionStatus conversionStatus = ConversionStatus.PENDING;
    
    private Integer conversionProgress = 0; // 0-100 percentage
    
    private String conversionError;
    
    @Column(nullable = false)
    private Integer length; // in seconds
    
    private String genre;
    private String description;
    
    @Column(nullable = false)
    private Integer seasonNumber;
    
    @Column(nullable = false)
    private Integer episodeNumber;
    
    @ManyToOne
    @JoinColumn(name = "playlist_id")
    @JsonBackReference("playlist-videos")
    private Playlist playlist;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    @JsonBackReference("user-videos")
    private User uploadedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        log.debug("Creating new video with title: {}", title);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        log.debug("Updating video: {}", id);
    }

    @Override
    public String toString() {
        return this.id;
    }
}