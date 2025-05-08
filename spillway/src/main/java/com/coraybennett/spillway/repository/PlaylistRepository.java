package com.coraybennett.spillway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.Playlist;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, String> {
}