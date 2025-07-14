package com.coraybennett.spillway.service.impl;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.VideoConversionService;

/**
 * Extended interface for video conversion service with encryption support.
 */
public interface EncryptedVideoConversionService extends VideoConversionService {
    
    /**
     * Converts a video file to HLS format with encryption.
     * 
     * @param sourceFile The path to the source video file
     * @param video The Video entity associated with this file
     * @param encryptionKey The encryption key to use (null for unencrypted videos)
     * @return CompletableFuture that completes when the conversion is done
     */
    CompletableFuture<Void> convertToHls(Path sourceFile, Video video, String encryptionKey);
}