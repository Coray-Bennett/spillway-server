package com.coraybennett.spillway.repository;

import com.coraybennett.spillway.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    List<Video> findByPlaylistId(String playlistId);
    List<Video> findByPlaylistIdOrderBySeasonNumberAscEpisodeNumberAsc(String playlistId);
}