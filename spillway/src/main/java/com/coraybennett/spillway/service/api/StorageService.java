package com.coraybennett.spillway.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface defining file storage operations.
 */
public interface StorageService {
    
    /**
     * Initializes the storage system.
     * 
     * @throws IOException if initialization fails
     */
    void initialize() throws IOException;
    
    /**
     * Stores a file from a MultipartFile.
     * 
     * @param file The file to store
     * @param destinationPath Path where the file should be stored
     * @return Path to the stored file
     * @throws IOException if storing fails
     */
    Path store(MultipartFile file, String destinationPath) throws IOException;
    
    /**
     * Stores a file from an InputStream.
     * 
     * @param inputStream The input stream to read from
     * @param filename The filename to use
     * @param destinationPath Path where the file should be stored
     * @return Path to the stored file
     * @throws IOException if storing fails
     */
    Path store(InputStream inputStream, String filename, String destinationPath) throws IOException;
    
    /**
     * Loads a file as a Resource.
     * 
     * @param path Path to the file
     * @return Resource for the file
     */
    Resource loadAsResource(Path path);
    
    /**
     * Deletes a file or directory.
     * 
     * @param path Path to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean delete(Path path);
    
    /**
     * Checks if a file exists.
     * 
     * @param path Path to check
     * @return true if the file exists, false otherwise
     */
    boolean exists(Path path);
}