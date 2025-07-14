package com.coraybennett.spillway.service.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.coraybennett.spillway.service.api.VideoEncryptionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of VideoEncryptionService using AES-GCM encryption.
 */
@Service
@Slf4j
public class DefaultVideoEncryptionService implements VideoEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int BUFFER_SIZE = 8192;
    
    @Override
    public String generateEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("Failed to generate encryption key", e);
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
    
    @Override
    public void encryptFile(Path inputPath, Path outputPath, String encryptionKey) throws Exception {
        log.debug("Encrypting file from {} to {}", inputPath, outputPath);
        
        SecretKey secretKey = getSecretKey(encryptionKey);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        // Ensure output directory exists
        Files.createDirectories(outputPath.getParent());
        
        try (FileInputStream fis = new FileInputStream(inputPath.toFile());
             FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            
            // Write IV to the beginning of the file
            fos.write(iv);
            
            // Encrypt the file content
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
        }
        
        log.debug("File encrypted successfully");
    }
    
    @Override
    public byte[] decryptFile(Path inputPath, String encryptionKey) throws Exception {
        log.debug("Decrypting file from {}", inputPath);
        
        SecretKey secretKey = getSecretKey(encryptionKey);
        
        byte[] encryptedData = Files.readAllBytes(inputPath);
        
        // Extract IV from the beginning of the file
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        
        // Extract the actual encrypted content
        byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        return cipher.doFinal(cipherText);
    }
    
    @Override
    public byte[] encryptData(byte[] data, String encryptionKey) throws Exception {
        SecretKey secretKey = getSecretKey(encryptionKey);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        byte[] encryptedData = cipher.doFinal(data);
        
        // Combine IV and encrypted data
        byte[] combined = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, combined, GCM_IV_LENGTH, encryptedData.length);
        
        return combined;
    }
    
    @Override
    public byte[] decryptData(byte[] encryptedData, String encryptionKey) throws Exception {
        if (encryptedData == null || encryptedData.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        
        SecretKey secretKey = getSecretKey(encryptionKey);
        
        // Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        
        // Extract cipher text
        byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        return cipher.doFinal(cipherText);
    }
    
    @Override
    public boolean isValidKey(String encryptionKey) {
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            // AES-256 key should be 32 bytes
            return keyBytes.length == KEY_SIZE / 8;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid base64 encoding for key", e);
            return false;
        }
    }
    
    @Override
    public SecretKey getSecretKey(String encryptionKey) throws Exception {
        if (!isValidKey(encryptionKey)) {
            throw new IllegalArgumentException("Invalid encryption key");
        }
        
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITHM);
    }
}