package com.coraybennett.spillway.service.enums;

import java.util.Arrays;

/**
 * QualityLevel class for video resolutions (e.g. 1920x1080)
 */
public class QualityLevel {
    public final String name;
    public final int height;
    public final int width;
    public final String bitrate;
    public final String maxRate;
    public final String bufSize;
    public final String audioBitrate;
    public final int bandwidth;
    
    /**
     * Creates a new QualityLevel with specific encoding parameters
     */
    private QualityLevel(String name, int height, int width, String bitrate, String maxRate, String bufSize, 
                String audioBitrate, int bandwidth) {
        this.name = name;
        this.height = height;
        this.width = width;
        this.bitrate = bitrate;
        this.maxRate = maxRate;
        this.bufSize = bufSize;
        this.audioBitrate = audioBitrate;
        this.bandwidth = bandwidth;
    }
    
    /**
     * Gets the best QualityLevel for a given resolution
     * 
     * @param width Source width
     * @param height Source height
     * @return The best quality level that doesn't exceed the source resolution
     */
    public static QualityLevel getBestQualityForResolution(int width, int height) {
        int maxDimension = Math.max(width, height);
        
        return Arrays.stream(ALL_QUALITY_LEVELS)
            .filter(quality -> quality.height <= maxDimension)
            .findFirst()
            .orElse(ALL_QUALITY_LEVELS[ALL_QUALITY_LEVELS.length - 1]); // Default to lowest if none match
    }
    
    /**
     * Gets the closest valid quality level for a specific height
     * 
     * @param targetHeight Target height to find
     * @return The closest quality level
     */
    public static QualityLevel getQualityByHeight(int targetHeight) {
        return Arrays.stream(ALL_QUALITY_LEVELS)
            .reduce((a, b) -> Math.abs(a.height - targetHeight) < Math.abs(b.height - targetHeight) ? a : b)
            .orElse(ALL_QUALITY_LEVELS[ALL_QUALITY_LEVELS.length - 1]);
    }
    
    /**
     * Finds a quality level by name
     * 
     * @param name The name to search for (e.g., "720p")
     * @return The matching quality level or null if not found
     */
    public static QualityLevel getByName(String name) {
        return Arrays.stream(ALL_QUALITY_LEVELS)
            .filter(quality -> quality.name.equals(name))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Determines if a quality level is HD (720p or higher)
     * 
     * @return true if HD quality
     */
    public boolean isHD() {
        return this.height >= 720;
    }
    
    /**
     * Determines if a quality level is 4K (2160p or higher)
     * 
     * @return true if 4K quality
     */
    public boolean is4K() {
        return this.height >= 2160;
    }

    @Override
    public String toString() {
        return name + " (" + width + "x" + height + ")";
    }

    // Define all possible quality levels in descending order of quality
    public static final QualityLevel[] ALL_QUALITY_LEVELS = {
        new QualityLevel("2160p", 2160, 3840, "8000k", "8500k", "12000k", "192k", 8500000),
        new QualityLevel("1080p", 1080, 1920, "5000k", "5350k", "7500k", "192k", 5350000),
        new QualityLevel("720p", 720, 1280, "2500k", "2675k", "3750k", "128k", 2675000),
        new QualityLevel("480p", 480, 854, "1000k", "1075k", "1500k", "128k", 1075000),
        new QualityLevel("360p", 360, 640, "500k", "538k", "750k", "96k", 538000)
    };
    
    // Predefined constants for convenience
    public static final QualityLevel UHD = ALL_QUALITY_LEVELS[0];  // 2160p
    public static final QualityLevel FULL_HD = ALL_QUALITY_LEVELS[1];  // 1080p
    public static final QualityLevel HD = ALL_QUALITY_LEVELS[2];  // 720p
    public static final QualityLevel SD = ALL_QUALITY_LEVELS[3];  // 480p
    public static final QualityLevel LOW = ALL_QUALITY_LEVELS[4];  // 360p
}