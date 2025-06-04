package com.coraybennett.spillway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.VideoShare;

/**
 * Repository for managing video shares.
 */
@Repository
public interface VideoShareRepository extends JpaRepository<VideoShare, String> {
    
    /**
     * Find all shares for a specific video.
     */
    List<VideoShare> findByVideoId(String videoId);
    
    /**
     * Find all shares created by a specific user.
     */
    List<VideoShare> findBySharedByUsernameOrderByCreatedAtDesc(String username);
    
    /**
     * Find all shares for a specific user (videos shared with them).
     */
    List<VideoShare> findBySharedWithUsernameOrderByCreatedAtDesc(String username);
    
    /**
     * Find a specific share between users for a video.
     */
    Optional<VideoShare> findByVideoIdAndSharedByIdAndSharedWithId(
        String videoId, String sharedById, String sharedWithId);
    
    /**
     * Find all active and valid shares for a specific video and user.
     */
    @Query("SELECT vs FROM VideoShare vs WHERE vs.video.id = :videoId " +
           "AND vs.sharedWith.id = :userId AND vs.active = true " +
           "AND (vs.expiresAt IS NULL OR vs.expiresAt > CURRENT_TIMESTAMP)")
    List<VideoShare> findValidSharesForUser(@Param("videoId") String videoId, 
                                            @Param("userId") String userId);
    
    /**
     * Find all active shares for a user (videos shared with them).
     */
    @Query("SELECT vs FROM VideoShare vs WHERE vs.sharedWith.id = :userId " +
           "AND vs.active = true " +
           "AND (vs.expiresAt IS NULL OR vs.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY vs.createdAt DESC")
    List<VideoShare> findAllValidSharesForUser(@Param("userId") String userId);
    
    /**
     * Check if a specific video is shared with a user.
     */
    @Query("SELECT COUNT(vs) > 0 FROM VideoShare vs WHERE vs.video.id = :videoId " +
           "AND vs.sharedWith.id = :userId AND vs.active = true " +
           "AND (vs.expiresAt IS NULL OR vs.expiresAt > CURRENT_TIMESTAMP)")
    boolean isVideoSharedWithUser(@Param("videoId") String videoId, 
                                  @Param("userId") String userId);
}