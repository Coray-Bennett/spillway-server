package com.coraybennett.spillway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, String> {
    List<Playlist> findByCreatedBy(User user);
    List<Playlist> findByCreatedById(String userId);
}