package com.coraybennett.spillway.specification;

import org.springframework.data.jpa.domain.Specification;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.dto.PlaylistSearchRequest;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistSpecification {
    
    public static Specification<Playlist> buildSpecification(PlaylistSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // General query search (name or description)
            if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                String searchPattern = "%" + request.getQuery().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(namePredicate, descriptionPredicate));
            }
            
            // Name filter
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), 
                    "%" + request.getName().toLowerCase() + "%"));
            }
            
            // Description filter
            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), 
                    "%" + request.getDescription().toLowerCase() + "%"));
            }
            
            // Creator filter
            if (request.getCreatedBy() != null && !request.getCreatedBy().trim().isEmpty()) {
                Join<Playlist, User> userJoin = root.join("createdBy", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(userJoin.get("username")), 
                    request.getCreatedBy().toLowerCase()));
            }
            
            // Date filters
            if (request.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), request.getCreatedAfter()));
            }
            if (request.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), request.getCreatedBefore()));
            }
            
            // Video count filters
            if (request.getMinVideoCount() != null || request.getMaxVideoCount() != null) {
                // For count filtering, we need to use a subquery
                Subquery<Long> videoCountSubquery = query.subquery(Long.class);
                Root<Playlist> subRoot = videoCountSubquery.from(Playlist.class);
                Join<?, ?> videosJoin = subRoot.join("videos", JoinType.LEFT);
                
                videoCountSubquery.select(criteriaBuilder.count(videosJoin))
                    .where(criteriaBuilder.equal(subRoot.get("id"), root.get("id")));
                
                if (request.getMinVideoCount() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        videoCountSubquery, request.getMinVideoCount().longValue()));
                }
                if (request.getMaxVideoCount() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        videoCountSubquery, request.getMaxVideoCount().longValue()));
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}