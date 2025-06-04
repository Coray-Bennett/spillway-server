package com.coraybennett.spillway.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
@Slf4j
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private boolean emailConfirmed = false;
    
    @Column(unique = true)
    private String confirmationToken;
    
    @Column
    private LocalDateTime confirmationTokenExpiry;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private boolean enabled = false;

    @OneToMany(mappedBy = "uploadedBy")
    @JsonManagedReference("user-videos")
    private List<Video> uploadedVideos;

    @OneToMany(mappedBy = "createdBy")
    @JsonManagedReference("user-playlists")
    private List<Playlist> createdPlaylists;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        log.debug("Creating new user with username: {}", username);
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}