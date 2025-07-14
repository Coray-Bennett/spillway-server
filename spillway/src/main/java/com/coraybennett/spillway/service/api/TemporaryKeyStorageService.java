package com.coraybennett.spillway.service.api;

/**
 * Interface for temporarily storing encryption keys during video upload and conversion.
 * Keys should be automatically deleted after a short period or after use.
 */
public interface TemporaryKeyStorageService {
    
    /**
     * Store a temporary encryption key for a video.
     * 
     * @param videoId The ID of the video
     * @param encryptionKey The encryption key to store
     * @param ttlSeconds Time-to-live in seconds (default if null)
     */
    void storeKey(String videoId, String encryptionKey, Integer ttlSeconds);
    
    /**
     * Retrieve a temporary encryption key for a video.
     * 
     * @param videoId The ID of the video
     * @return The encryption key, or null if not found or expired
     */
    String retrieveKey(String videoId);
    
    /**
     * Delete a temporary encryption key.
     * 
     * @param videoId The ID of the video
     */
    void deleteKey(String videoId);
    
    /**
     * Check if a key exists for a video.
     * 
     * @param videoId The ID of the video
     * @return true if key exists and hasn't expired
     */
    boolean hasKey(String videoId);
}