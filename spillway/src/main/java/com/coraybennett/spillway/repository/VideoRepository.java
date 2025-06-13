package com.coraybennett.spillway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, String>, JpaSpecificationExecutor<Video> {
    
    /**
     * Find video by ID with uploadedBy and playlist relationships loaded.
     * This ensures the Video entity is fully populated for the metadata endpoint.
     */
    @EntityGraph(attributePaths = {"uploadedBy", "playlist"})
    Optional<Video> findWithRelationshipsById(String id);
    
    /**
     * Find video by ID with just the uploadedBy relationship loaded.
     */
    @Query("SELECT v FROM Video v LEFT JOIN FETCH v.uploadedBy WHERE v.id = :id")
    Optional<Video> findByIdWithUploader(@Param("id") String id);
    
    List<Video> findByPlaylistId(String playlistId);
    List<Video> findByPlaylistIdOrderBySeasonNumberAscEpisodeNumberAsc(String playlistId);
    List<Video> findByUploadedBy(User user);
    
    /**
     * Find videos by uploader ID with relationships loaded.
     */
    @EntityGraph(attributePaths = {"uploadedBy", "playlist"})
    List<Video> findByUploadedById(String userId);
    
    // Additional query methods for popular searches
    @Query("SELECT DISTINCT v.genre FROM Video v WHERE v.genre IS NOT NULL ORDER BY v.genre")
    List<String> findAllGenres();

    @Query("SELECT DISTINCT v.genre FROM Video v WHERE v.genre IS NOT NULL AND v.uploadedBy.id = :userId ORDER BY v.genre")
    List<String> findAllGenresByUploadedById(String userId);
    
    @Query("SELECT v FROM Video v LEFT JOIN FETCH v.uploadedBy WHERE v.conversionStatus = 'COMPLETED' ORDER BY v.createdAt DESC")
    List<Video> findRecentlyAddedVideos(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Find all videos by user with relationships loaded.
     */
    @Query("SELECT v FROM Video v LEFT JOIN FETCH v.uploadedBy LEFT JOIN FETCH v.playlist WHERE v.uploadedBy.id = :userId ORDER BY v.createdAt DESC")
    List<Video> findAllByUserIdWithRelationships(@Param("userId") String userId);
}