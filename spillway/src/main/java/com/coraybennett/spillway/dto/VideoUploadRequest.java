package com.coraybennett.spillway.dto;

import com.coraybennett.spillway.model.VideoType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadRequest {
    private String title;
    private VideoType type;
    private Integer length;
    private String genre;
    private String description;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private String playlistId;
}