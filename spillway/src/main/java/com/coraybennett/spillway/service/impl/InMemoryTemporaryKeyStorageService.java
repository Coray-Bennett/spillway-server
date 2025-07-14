package com.coraybennett.spillway.service.impl;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.coraybennett.spillway.service.api.TemporaryKeyStorageService;

import lombok.extern.slf4j.Slf4j;

/**
 * In-memory implementation of TemporaryKeyStorageService.
 * Keys are stored temporarily with automatic expiration.
 * Note: This implementation is not suitable for distributed systems.
 * For production, consider using Redis or another distributed cache.
 */
@Service
@Slf4j
public class InMemoryTemporaryKeyStorageService implements TemporaryKeyStorageService {
    
    private static final int DEFAULT_TTL_SECONDS = 3600; // 1 hour
    
    private final Map<String, KeyEntry> keyStore = new ConcurrentHashMap<>();
    
    /**
     * Internal class to store key with expiration time.
     */
    private static class KeyEntry {
        private final String key;
        private final Instant expiresAt;
        
        public KeyEntry(String key, Instant expiresAt) {
            this.key = key;
            this.expiresAt = expiresAt;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    @Override
    public void storeKey(String videoId, String encryptionKey, Integer ttlSeconds) {
        if (videoId == null || encryptionKey == null) {
            throw new IllegalArgumentException("Video ID and encryption key must not be null");
        }
        
        int ttl = ttlSeconds != null ? ttlSeconds : DEFAULT_TTL_SECONDS;
        Instant expiresAt = Instant.now().plusSeconds(ttl);
        
        keyStore.put(videoId, new KeyEntry(encryptionKey, expiresAt));
        log.debug("Stored temporary key for video {} with TTL {} seconds", videoId, ttl);
    }
    
    @Override
    public String retrieveKey(String videoId) {
        if (videoId == null) {
            return null;
        }
        
        KeyEntry entry = keyStore.get(videoId);
        if (entry == null) {
            log.debug("No key found for video {}", videoId);
            return null;
        }
        
        if (entry.isExpired()) {
            keyStore.remove(videoId);
            log.debug("Key for video {} has expired", videoId);
            return null;
        }
        
        log.debug("Retrieved key for video {}", videoId);
        return entry.key;
    }
    
    @Override
    public void deleteKey(String videoId) {
        if (videoId != null) {
            KeyEntry removed = keyStore.remove(videoId);
            if (removed != null) {
                log.debug("Deleted key for video {}", videoId);
            }
        }
    }
    
    @Override
    public boolean hasKey(String videoId) {
        if (videoId == null) {
            return false;
        }
        
        KeyEntry entry = keyStore.get(videoId);
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            keyStore.remove(videoId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Scheduled task to clean up expired keys.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void cleanupExpiredKeys() {
        log.debug("Running expired key cleanup");
        
        int removedCount = 0;
        for (Map.Entry<String, KeyEntry> entry : keyStore.entrySet()) {
            if (entry.getValue().isExpired()) {
                keyStore.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.info("Removed {} expired keys", removedCount);
        }
    }
    
    /**
     * Get the current number of stored keys (for monitoring).
     */
    public int getStoredKeyCount() {
        return keyStore.size();
    }
}