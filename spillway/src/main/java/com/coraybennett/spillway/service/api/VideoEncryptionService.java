package com.coraybennett.spillway.service.api;

import java.nio.file.Path;
import javax.crypto.SecretKey;

/**
 * Interface defining operations for video encryption and decryption.
 */
public interface VideoEncryptionService {
    
    /**
     * Generates a new symmetric encryption key.
     * 
     * @return A base64 encoded encryption key
     */
    String generateEncryptionKey();
    
    /**
     * Encrypts a video file or segment.
     * 
     * @param inputPath Path to the input file
     * @param outputPath Path where the encrypted file should be saved
     * @param encryptionKey Base64 encoded encryption key
     * @throws Exception if encryption fails
     */
    void encryptFile(Path inputPath, Path outputPath, String encryptionKey) throws Exception;
    
    /**
     * Decrypts a video file or segment.
     * 
     * @param inputPath Path to the encrypted file
     * @param encryptionKey Base64 encoded encryption key
     * @return Decrypted byte array
     * @throws Exception if decryption fails
     */
    byte[] decryptFile(Path inputPath, String encryptionKey) throws Exception;
    
    /**
     * Encrypts data in memory.
     * 
     * @param data The data to encrypt
     * @param encryptionKey Base64 encoded encryption key
     * @return Encrypted byte array
     * @throws Exception if encryption fails
     */
    byte[] encryptData(byte[] data, String encryptionKey) throws Exception;
    
    /**
     * Decrypts data in memory.
     * 
     * @param encryptedData The encrypted data
     * @param encryptionKey Base64 encoded encryption key
     * @return Decrypted byte array
     * @throws Exception if decryption fails
     */
    byte[] decryptData(byte[] encryptedData, String encryptionKey) throws Exception;
    
    /**
     * Validates an encryption key.
     * 
     * @param encryptionKey Base64 encoded encryption key to validate
     * @return true if the key is valid, false otherwise
     */
    boolean isValidKey(String encryptionKey);
    
    /**
     * Creates a SecretKey from a base64 encoded string.
     * 
     * @param encryptionKey Base64 encoded encryption key
     * @return SecretKey object
     * @throws Exception if key creation fails
     */
    SecretKey getSecretKey(String encryptionKey) throws Exception;
}