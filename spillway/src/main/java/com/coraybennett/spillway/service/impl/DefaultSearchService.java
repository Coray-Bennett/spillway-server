package com.coraybennett.spillway.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coraybennett.spillway.dto.VideoSearchRequest;
import com.coraybennett.spillway.dto.PlaylistSearchRequest;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.repository.PlaylistRepository;
import com.coraybennett.spillway.service.api.SearchService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.specification.VideoSpecification;
import com.coraybennett.spillway.specification.PlaylistSpecification;

/**
 * Default implementation of SearchService with access control.
 */
@Service
@Transactional(readOnly = true)
public class DefaultSearchService implements SearchService {
    
    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    private final VideoAccessService videoAccessService;
    
    @Autowired
    public DefaultSearchService(
            VideoRepository videoRepository, 
            PlaylistRepository playlistRepository,
            VideoAccessService videoAccessService) {
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
        this.videoAccessService = videoAccessService;
    }
    
    @Override
    public Page<Video> searchVideos(VideoSearchRequest request, User user) {
        // Get the base search specification
        Specification<Video> searchSpec = VideoSpecification.buildSpecification(request);
        
        // Get the access control specification
        Specification<Video> accessSpec = videoAccessService.getVideoAccessSpecification(user);
        
        // Combine specifications: search criteria AND access control
        Specification<Video> combinedSpec = Specification.where(accessSpec).and(searchSpec);
        
        // Build the pageable with sorting
        Pageable pageable = createPageable(
            request.getPage(), 
            request.getSize(),
            request.getSortBy(),
            request.getSortDirection(),
            "createdAt"
        );
        
        // Execute the query
        return videoRepository.findAll(combinedSpec, pageable);
    }
    
    @Override
    public Page<Playlist> searchPlaylists(PlaylistSearchRequest request, User user) {
        // Get the base search specification
        Specification<Playlist> searchSpec = PlaylistSpecification.buildSpecification(request);
        
        // Get the access control specification
        Specification<Playlist> accessSpec = videoAccessService.getPlaylistAccessSpecification(user);
        
        // Combine specifications: search criteria AND access control
        Specification<Playlist> combinedSpec = Specification.where(accessSpec).and(searchSpec);
        
        // Build the pageable with sorting
        Pageable pageable = createPageable(
            request.getPage(),
            request.getSize(),
            request.getSortBy(),
            request.getSortDirection(),
            "createdAt"
        );
        
        // Execute the query
        return playlistRepository.findAll(combinedSpec, pageable);
    }
    
    @Override
    public List<String> getAllGenres(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        
        // Get genres only from videos the user has access to
        return videoRepository.findAllGenresByUploadedById(user.getId());
    }
    
    @Override
    public List<Video> getRecentlyAddedVideos(int limit, User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Get the access control specification
        Specification<Video> accessSpec = videoAccessService.getVideoAccessSpecification(user);
        
        // Add filter for completed videos only
        Specification<Video> completedSpec = (root, query, cb) -> 
            cb.equal(root.get("conversionStatus"), com.coraybennett.spillway.model.ConversionStatus.COMPLETED);
        
        // Combine specifications
        Specification<Video> combinedSpec = Specification.where(accessSpec).and(completedSpec);
        
        return videoRepository.findAll(combinedSpec, pageable).getContent();
    }
    
    @Override
    public List<Playlist> getMostPopularPlaylists(int limit, User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        
        Pageable pageable = PageRequest.of(0, limit);
        
        // Get playlists created by the user, ordered by video count
        return playlistRepository.findMostPopularPlaylistsByCreatedById(user.getId(), pageable);
    }
    
    /**
     * Creates a Pageable object with sorting.
     */
    private Pageable createPageable(Integer page, Integer size, String sortBy, 
                                  String sortDirection, String defaultSortField) {
        // Validate and set defaults
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;
        
        // Determine sort field
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : defaultSortField;
        
        // Map sort field names for videos
        sortField = mapSortField(sortField);
        
        // Determine sort direction
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        // Create sort
        Sort sort = Sort.by(direction, sortField);
        
        return PageRequest.of(pageNumber, pageSize, sort);
    }
    
    /**
     * Maps user-friendly sort field names to entity field names.
     */
    private String mapSortField(String sortBy) {
        if (sortBy == null) {
            return "createdAt";
        }
        
        switch (sortBy.toLowerCase()) {
            case "title":
            case "name":
                return "title";
            case "uploaddate":
            case "createddate":
                return "createdAt";
            case "length":
            case "duration":
                return "length";
            case "videocount":
                return "videos.size";
            default:
                return sortBy;
        }
    }
}