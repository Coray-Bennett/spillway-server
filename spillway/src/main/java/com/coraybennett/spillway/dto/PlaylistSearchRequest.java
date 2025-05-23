package com.coraybennett.spillway.dto;

import java.time.LocalDateTime;

public class PlaylistSearchRequest {
    private String query; // General search query for name/description
    private String name;
    private String description;
    private String createdBy; // username
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private Integer minVideoCount;
    private Integer maxVideoCount;
    private String sortBy; // name, createdDate, videoCount
    private String sortDirection; // ASC, DESC
    private Integer page = 0;
    private Integer size = 20;
    
    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAfter() { return createdAfter; }
    public void setCreatedAfter(LocalDateTime createdAfter) { this.createdAfter = createdAfter; }
    
    public LocalDateTime getCreatedBefore() { return createdBefore; }
    public void setCreatedBefore(LocalDateTime createdBefore) { this.createdBefore = createdBefore; }
    
    public Integer getMinVideoCount() { return minVideoCount; }
    public void setMinVideoCount(Integer minVideoCount) { this.minVideoCount = minVideoCount; }
    
    public Integer getMaxVideoCount() { return maxVideoCount; }
    public void setMaxVideoCount(Integer maxVideoCount) { this.maxVideoCount = maxVideoCount; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = Math.min(size, 100); } // Cap at 100
}