package com.coraybennett.spillway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    List<Video> findByPlaylistId(String playlistId);
    List<Video> findByPlaylistIdOrderBySeasonNumberAscEpisodeNumberAsc(String playlistId);
    List<Video> findByUploadedBy(User user);
    List<Video> findByUploadedById(String userId);
}