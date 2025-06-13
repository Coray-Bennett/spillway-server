package com.coraybennett.spillway.dto;

/**
 * Response DTO for playlist video addition
 */
public record PlaylistVideoAddResponse(
        String playlistId,
        String videoId,
        String message
) {}