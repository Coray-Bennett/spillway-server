package com.coraybennett.spillway.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, String>, JpaSpecificationExecutor<Playlist> {
    List<Playlist> findByCreatedBy(User user);
    List<Playlist> findByCreatedById(String userId);
    
    @Query("SELECT p FROM Playlist p LEFT JOIN FETCH p.videos WHERE p.id = :id")
    Playlist findByIdWithVideos(String id);
    
    @Query("SELECT p FROM Playlist p ORDER BY SIZE(p.videos) DESC")
    List<Playlist> findMostPopularPlaylists(Pageable pageable);

    @Query("SELECT p FROM Playlist p WHERE p.createdBy.id = :userId ORDER BY SIZE(p.videos) DESC")
    List<Playlist> findMostPopularPlaylistsByCreatedById(String userId, Pageable pageable);
}
