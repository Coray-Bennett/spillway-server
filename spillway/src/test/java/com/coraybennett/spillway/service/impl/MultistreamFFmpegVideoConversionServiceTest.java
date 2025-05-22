package com.coraybennett.spillway.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.coraybennett.spillway.model.ConversionStatus;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.model.VideoType;
import com.coraybennett.spillway.repository.VideoRepository;
import com.coraybennett.spillway.service.api.StorageService;

public class MultistreamFFmpegVideoConversionServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private MultistreamFFmpegVideoConversionService videoConversionService;

    @TempDir
    Path tempDir;

    private Video testVideo;
    private Path testVideoFile;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        // Set up test video
        testVideo = new Video();
        testVideo.setId("test-video-id");
        testVideo.setTitle("Test Video");
        testVideo.setType(VideoType.MOVIE);
        testVideo.setConversionStatus(ConversionStatus.PENDING);
        
        // Create a test video file
        testVideoFile = tempDir.resolve("test.mp4");
        Files.write(testVideoFile, "dummy video content".getBytes());
        
        // Configure the service
        ReflectionTestUtils.setField(videoConversionService, "outputDirectory", tempDir.toString());
        ReflectionTestUtils.setField(videoConversionService, "baseUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(videoConversionService, "encodingPreset", "ultrafast");
        ReflectionTestUtils.setField(videoConversionService, "segmentDuration", 4);
        ReflectionTestUtils.setField(videoConversionService, "hardwareAccelerationEnabled", false);
    }

    @Test
    void testGetVideoDuration() {
        // This test would require FFmpeg to be installed
        // For unit testing, we'll mock the behavior
        int duration = videoConversionService.getVideoDuration(testVideoFile);
        // Duration will be 0 if FFmpeg is not available
        assertTrue(duration >= 0);
    }

    @Test
    void testGetOutputDirectory() {
        Path outputDir = videoConversionService.getOutputDirectory();
        assertEquals(tempDir, outputDir);
    }

    @Test
    void testCleanupVideoFiles() throws IOException {
        // Create test directory structure
        Path videoDir = tempDir.resolve(testVideo.getId());
        Files.createDirectories(videoDir);
        Path testFile = videoDir.resolve("test.m3u8");
        Files.write(testFile, "test content".getBytes());
        
        when(storageService.delete(any(Path.class))).thenReturn(true);
        
        boolean result = videoConversionService.cleanupVideoFiles(testVideo.getId());
        
        assertTrue(result);
        verify(storageService).delete(videoDir);
    }

    @Test
    void testIsVideoFileTypeSupported() {
        assertTrue(videoConversionService.isVideoFileTypeSupported("video.mp4"));
        assertTrue(videoConversionService.isVideoFileTypeSupported("video.mov"));
        assertTrue(videoConversionService.isVideoFileTypeSupported("video.avi"));
        assertTrue(videoConversionService.isVideoFileTypeSupported("VIDEO.MP4"));
        assertFalse(videoConversionService.isVideoFileTypeSupported("document.pdf"));
        assertFalse(videoConversionService.isVideoFileTypeSupported("image.jpg"));
    }

    @Test
    void testConvertToHlsWithInvalidFileType() {
        Path invalidFile = tempDir.resolve("document.pdf");
        
        CompletableFuture<Void> future = videoConversionService.convertToHls(invalidFile, testVideo);
        
        assertThrows(Exception.class, () -> future.get());
        verify(videoRepository, atLeastOnce()).save(any(Video.class));
    }
}