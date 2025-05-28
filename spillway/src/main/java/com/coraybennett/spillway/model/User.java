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

@Entity
@Table(name = "users")
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
    }

    public User() {}

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isEmailConfirmed() { return emailConfirmed; }
    public void setEmailConfirmed(boolean emailConfirmed) { this.emailConfirmed = emailConfirmed; }
    public String getConfirmationToken() { return confirmationToken; }
    public void setConfirmationToken(String confirmationToken) { this.confirmationToken = confirmationToken; }
    public LocalDateTime getConfirmationTokenExpiry() { return confirmationTokenExpiry; }
    public void setConfirmationTokenExpiry(LocalDateTime confirmationTokenExpiry) { this.confirmationTokenExpiry = confirmationTokenExpiry; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<Video> getUploadedVideos() { return uploadedVideos; }
    public void setUploadedVideos(List<Video> uploadedVideos) { this.uploadedVideos = uploadedVideos; }
    public List<Playlist> getCreatedPlaylists() { return createdPlaylists; }
    public void setCreatedPlaylists(List<Playlist> createdPlaylists) { this.createdPlaylists = createdPlaylists; }
}