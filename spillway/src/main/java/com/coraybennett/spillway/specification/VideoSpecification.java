package com.coraybennett.spillway.specification;

import org.springframework.data.jpa.domain.Specification;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.dto.VideoSearchRequest;

import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VideoSpecification {
    
    public static Specification<Video> buildSpecification(VideoSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // General query search (title or description)
            if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                String searchPattern = "%" + request.getQuery().toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(titlePredicate, descriptionPredicate));
            }
            
            // Title filter
            if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), 
                    "%" + request.getTitle().toLowerCase() + "%"));
            }
            
            // Genre filter
            if (request.getGenre() != null && !request.getGenre().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("genre")), 
                    request.getGenre().toLowerCase()));
            }
            
            // Video type filter
            if (request.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), request.getType()));
            }
            
            // Conversion status filter
            if (request.getConversionStatus() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("conversionStatus"), request.getConversionStatus()));
            }
            
            // Length filters
            if (request.getMinLength() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("length"), request.getMinLength()));
            }
            if (request.getMaxLength() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("length"), request.getMaxLength()));
            }
            
            // Date filters
            if (request.getUploadedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), request.getUploadedAfter()));
            }
            if (request.getUploadedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), request.getUploadedBefore()));
            }
            
            // Uploader filter
            if (request.getUploadedBy() != null && !request.getUploadedBy().trim().isEmpty()) {
                Join<Video, User> userJoin = root.join("uploadedBy", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(userJoin.get("username")), 
                    request.getUploadedBy().toLowerCase()));
            }
            
            // Playlist filter
            if (request.getPlaylistId() != null && !request.getPlaylistId().trim().isEmpty()) {
                Join<Video, Playlist> playlistJoin = root.join("playlist", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(playlistJoin.get("id"), request.getPlaylistId()));
            }
            
            // Season/Episode filters
            if (request.getSeasonNumber() != null) {
                predicates.add(criteriaBuilder.equal(root.get("seasonNumber"), request.getSeasonNumber()));
            }
            if (request.getEpisodeNumber() != null) {
                predicates.add(criteriaBuilder.equal(root.get("episodeNumber"), request.getEpisodeNumber()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
