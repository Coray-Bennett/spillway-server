package com.coraybennett.spillway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, String>, JpaSpecificationExecutor<Video> {
    List<Video> findByPlaylistId(String playlistId);
    List<Video> findByPlaylistIdOrderBySeasonNumberAscEpisodeNumberAsc(String playlistId);
    List<Video> findByUploadedBy(User user);
    List<Video> findByUploadedById(String userId);
    
    // Additional query methods for popular searches
    @Query("SELECT DISTINCT v.genre FROM Video v WHERE v.genre IS NOT NULL ORDER BY v.genre")
    List<String> findAllGenres();

    @Query("SELECT DISTINCT v.genre FROM Video v WHERE v.genre IS NOT NULL AND v.uploadedBy.id = :userId ORDER BY v.genre")
    List<String> findAllGenresByUploadedById(String userId);

    
    @Query("SELECT v FROM Video v WHERE v.conversionStatus = 'COMPLETED' ORDER BY v.createdAt DESC")
    List<Video> findRecentlyAddedVideos(org.springframework.data.domain.Pageable pageable);
}
