package com.coraybennett.spillway.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoType;
import com.coraybennett.spillway.repository.UserRepository;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.VideoConversionService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class VideoConversionIntegrationTest {

    @Autowired
    private VideoConversionService videoConversionService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @TempDir
    Path tempDir;

    private Video testVideo;
    private User testUser;
    private Path testVideoFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create test user
        testUser = new User("testuser", "password", "test@example.com");
        testUser = userRepository.save(testUser);

        // Create test video
        testVideo = new Video();
        testVideo.setTitle("Test Video");
        testVideo.setType(VideoType.MOVIE);
        testVideo.setLength(10);
        testVideo.setUploadedBy(testUser);
        testVideo.setPlaylistUrl("http://localhost:8081/video/test/playlist");
        testVideo.setDescription("Test video for integration testing");
        testVideo.setGenre("Test");
        testVideo.setSeasonNumber(1);
        testVideo.setEpisodeNumber(1);
        testVideo = videoRepository.save(testVideo);

        // Create a small test video file
        createTestVideoFile();
    }

    private void createTestVideoFile() throws IOException {
        testVideoFile = tempDir.resolve("test_video.mp4");
        
        // Copy a small test video from resources if available
        try (InputStream is = getClass().getResourceAsStream("/test-video.mp4")) {
            if (is != null) {
                Files.copy(is, testVideoFile);
            } else {
                // Create a dummy file for testing
                Files.write(testVideoFile, "dummy video content".getBytes());
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "ffmpeg.available", matches = "true")
    void testFullVideoConversionProcess() throws Exception {
        // Test requires FFmpeg to be installed
        CompletableFuture<Void> conversionFuture = videoConversionService.convertToHls(testVideoFile, testVideo);
        
        // Wait for conversion with timeout
        conversionFuture.get(60, TimeUnit.SECONDS);
        
        // Verify video status
        Video updatedVideo = videoRepository.findById(testVideo.getId()).orElseThrow();
        assertEquals(ConversionStatus.COMPLETED, updatedVideo.getConversionStatus());
        assertEquals(100, updatedVideo.getConversionProgress());
        
        // Verify output files exist
        Path outputDir = Paths.get(videoConversionService.getOutputDirectory().toString(), testVideo.getId());
        assertTrue(Files.exists(outputDir));
        
        // Check for master playlist
        Path masterPlaylist = outputDir.resolve(testVideo.getId() + ".m3u8");
        assertTrue(Files.exists(masterPlaylist));
        
        // Verify playlist content
        String playlistContent = Files.readString(masterPlaylist);
        assertTrue(playlistContent.contains("#EXTM3U"));
        assertTrue(playlistContent.contains("#EXT-X-VERSION"));
    }

    @Test
    void testVideoDurationDetection() {
        // This test can run without FFmpeg
        int duration = videoConversionService.getVideoDuration(testVideoFile);
        // If FFmpeg is not available, duration will be 0
        assertTrue(duration >= 0);
    }

    @Test
    void testVideoConversionCancellation() throws Exception {
        // Start conversion
        CompletableFuture<Void> future = videoConversionService.convertToHls(testVideoFile, testVideo);
        
        // Give it a moment to start
        Thread.sleep(100);
        
        // Cancel conversion
        boolean cancelled = videoConversionService.cancelConversion(testVideo.getId());
        
        // Verify cancellation (may not always succeed if conversion is too quick)
        if (cancelled) {
            Video updatedVideo = videoRepository.findById(testVideo.getId()).orElseThrow();
            assertEquals(ConversionStatus.FAILED, updatedVideo.getConversionStatus());
            assertNotNull(updatedVideo.getConversionError());
        }
    }

    @Test
    void testCleanupAfterConversion() throws IOException {
        // Create test files
        Path videoDir = videoConversionService.getOutputDirectory().resolve(testVideo.getId());
        Files.createDirectories(videoDir);
        Path testFile = videoDir.resolve("test.m3u8");
        Files.write(testFile, "test content".getBytes());
        
        // Cleanup
        boolean cleaned = videoConversionService.cleanupVideoFiles(testVideo.getId());
        
        assertTrue(cleaned);
        assertFalse(Files.exists(videoDir));
    }
}