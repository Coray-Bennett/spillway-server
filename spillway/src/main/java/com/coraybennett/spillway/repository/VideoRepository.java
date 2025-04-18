package com.coraybennett.spillway.repository;

import com.coraybennett.spillway.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    Optional<Video> findByTag(String tag);
    List<Video> findByPlaylistId(String playlistId);
    List<Video> findByPlaylistIdOrderBySeasonNumberAscEpisodeNumberAsc(String playlistId);
}