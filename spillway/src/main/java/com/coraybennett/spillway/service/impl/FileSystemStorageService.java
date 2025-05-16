package com.coraybennett.spillway.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.service.api.StorageService;

/**
 * Implementation of StorageService that uses the local file system.
 */
@Service
public class FileSystemStorageService implements StorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorageService.class);

    @Override
    public void initialize() throws IOException {
        // Ensure base directories exist
        String[] directories = {"content", "temp", "temp/uploads"};
        for (String dir : directories) {
            Path path = Paths.get(dir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created directory: {}", path.toAbsolutePath());
            }
        }
    }

    @Override
    public Path store(MultipartFile file, String destinationPath) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }
        
        Path destinationDir = Paths.get(destinationPath);
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }
        
        String filename = sanitizeFilename(file.getOriginalFilename());
        Path destinationFile = destinationDir.resolve(filename);
        
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored file: {} in {}", filename, destinationPath);
            return destinationFile;
        }
    }

    @Override
    public Path store(InputStream inputStream, String filename, String destinationPath) throws IOException {
        Path destinationDir = Paths.get(destinationPath);
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }
        
        filename = sanitizeFilename(filename);
        Path destinationFile = destinationDir.resolve(filename);
        
        Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Stored file: {} in {}", filename, destinationPath);
        return destinationFile;
    }

    @Override
    public Resource loadAsResource(Path path) {
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load file: " + path, e);
        }
    }

    @Override
    public boolean delete(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a)) // Sort in reverse order to delete files before directories
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.warn("Failed to delete: " + p, e);
                        }
                    });
                return true;
            } else {
                return Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            logger.error("Failed to delete path: " + path, e);
            return false;
        }
    }

    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }
    
    /**
     * Sanitizes a filename to ensure it's safe for filesystem storage.
     * 
     * @param filename The filename to sanitize
     * @return Sanitized filename
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
