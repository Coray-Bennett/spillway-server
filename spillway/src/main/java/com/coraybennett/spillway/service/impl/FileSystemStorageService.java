package com.coraybennett.spillway.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coraybennett.spillway.service.api.StorageService;

/**
 * Enhanced implementation of StorageService that uses the local file system
 * with NIO optimizations for better performance.
 */
@Service
public class FileSystemStorageService implements StorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorageService.class);
    private static final Pattern FILENAME_SANITIZER = Pattern.compile("[^a-zA-Z0-9.-]");
    private static final int BUFFER_SIZE = 64 * 1024;
    
    // Cache for directory existence checks to avoid repeated filesystem operations
    private final ConcurrentHashMap<String, Boolean> directoryExistsCache = new ConcurrentHashMap<>();
    
    @Value("${storage.enable-nio-transfer:true}")
    private boolean enableNioTransfer;
    
    @Override
    public void initialize() throws IOException {
        // Ensure base directories exist
        String[] directories = {"content", "temp", "temp/uploads"};
        for (String dir : directories) {
            Path path = Paths.get(dir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                directoryExistsCache.put(path.toString(), true);
                logger.info("Created directory: {}", path.toAbsolutePath());
            } else {
                directoryExistsCache.put(path.toString(), true);
            }
        }
    }

    @Override
    public Path store(MultipartFile file, String destinationPath) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }
        
        ensureDirectoryExists(destinationPath);
        
        String filename = sanitizeFilename(file.getOriginalFilename());
        Path destinationFile = Paths.get(destinationPath, filename);
        
        if (enableNioTransfer) {
            try (ReadableByteChannel inChannel = Channels.newChannel(file.getInputStream());
                 FileChannel outChannel = FileChannel.open(destinationFile, 
                     StandardOpenOption.CREATE, 
                     StandardOpenOption.TRUNCATE_EXISTING, 
                     StandardOpenOption.WRITE)) {
                
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                
                while ((inChannel.read(buffer)) != -1) {
                    buffer.flip();
                    outChannel.write(buffer);
                    buffer.clear();
                }
                
                logger.info("Stored file: {} in {} using NIO", filename, destinationPath);
            }
        } else {
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Stored file: {} in {}", filename, destinationPath);
            }
        }
        
        return destinationFile;
    }

    @Override
    public Path store(InputStream inputStream, String filename, String destinationPath) throws IOException {
        ensureDirectoryExists(destinationPath);
        
        filename = sanitizeFilename(filename);
        Path destinationFile = Paths.get(destinationPath, filename);
        
        if (enableNioTransfer) {
            try (ReadableByteChannel inChannel = Channels.newChannel(inputStream);
                 FileChannel outChannel = FileChannel.open(destinationFile, 
                     StandardOpenOption.CREATE, 
                     StandardOpenOption.TRUNCATE_EXISTING, 
                     StandardOpenOption.WRITE)) {
                
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                
                while ((inChannel.read(buffer)) != -1) {
                    buffer.flip();
                    outChannel.write(buffer);
                    buffer.clear();
                }
                
                logger.info("Stored file: {} in {} using NIO", filename, destinationPath);
            }
        } else {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored file: {} in {}", filename, destinationPath);
        }
        
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
                    .sorted((a, b) -> -a.compareTo(b))
                    .parallel()
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            logger.warn("Failed to delete: {}", p, e);
                        }
                    });
                
                directoryExistsCache.remove(path.toString());
                return true;
            } else {
                boolean deleted = Files.deleteIfExists(path);
                if (deleted) {
                    logger.debug("Deleted file: {}", path);
                }
                return deleted;
            }
        } catch (IOException e) {
            logger.error("Failed to delete path: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean exists(Path path) {
        String pathStr = path.toString();
        Boolean exists = directoryExistsCache.get(pathStr);
        if (exists != null) {
            return exists;
        }
        
        return Files.exists(path);
    }
    
    /**
     * Ensures a directory exists, using an internal cache to avoid repeated checks.
     */
    private void ensureDirectoryExists(String directoryPath) throws IOException {
        if (directoryExistsCache.containsKey(directoryPath)) {
            return;
        }
        
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.debug("Created directory: {}", path.toAbsolutePath());
        }
        
        directoryExistsCache.put(directoryPath, true);
    }
    
    /**
     * Sanitizes a filename to ensure it's safe for filesystem storage.
     * Using pre-compiled pattern for better performance.
     * 
     * @param filename The filename to sanitize
     * @return Sanitized filename
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        return FILENAME_SANITIZER.matcher(filename).replaceAll("_");
    }
}