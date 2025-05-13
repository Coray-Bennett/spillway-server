package com.coraybennett.spillway.dto;

public class PlaylistVideoDetails {
    private Integer seasonNumber;
    private Integer episodeNumber;
    
    public Integer getSeasonNumber() {
        return seasonNumber;
    }
    
    public void setSeasonNumber(Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }
    
    public Integer getEpisodeNumber() {
        return episodeNumber;
    }
    
    public void setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }
}