package com.coraybennett.spillway.service.impl;

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
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.repository.PlaylistRepository;
import com.coraybennett.spillway.service.api.SearchService;
import com.coraybennett.spillway.specification.VideoSpecification;
import com.coraybennett.spillway.specification.PlaylistSpecification;

/**
 * Default implementation of SearchService.
 */
@Service
@Transactional(readOnly = true)
public class DefaultSearchService implements SearchService {
    
    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    
    @Autowired
    public DefaultSearchService(VideoRepository videoRepository, PlaylistRepository playlistRepository) {
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
    }
    
    @Override
    public Page<Video> searchVideos(VideoSearchRequest request) {
        // Build the specification from the request
        Specification<Video> spec = VideoSpecification.buildSpecification(request);
        
        // Build the pageable with sorting
        Pageable pageable = createPageable(
            request.getPage(), 
            request.getSize(),
            request.getSortBy(),
            request.getSortDirection(),
            "createdAt" // default sort field
        );
        
        // Execute the query
        return videoRepository.findAll(spec, pageable);
    }
    
    @Override
    public Page<Playlist> searchPlaylists(PlaylistSearchRequest request) {
        // Build the specification from the request
        Specification<Playlist> spec = PlaylistSpecification.buildSpecification(request);
        
        // Build the pageable with sorting
        Pageable pageable = createPageable(
            request.getPage(),
            request.getSize(),
            request.getSortBy(),
            request.getSortDirection(),
            "createdAt" // default sort field
        );
        
        // Execute the query
        return playlistRepository.findAll(spec, pageable);
    }
    
    @Override
    public List<String> getAllGenres() {
        return videoRepository.findAllGenres();
    }
    
    @Override
    public List<Video> getRecentlyAddedVideos(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return videoRepository.findRecentlyAddedVideos(pageable);
    }
    
    @Override
    public List<Playlist> getMostPopularPlaylists(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return playlistRepository.findMostPopularPlaylists(pageable);
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