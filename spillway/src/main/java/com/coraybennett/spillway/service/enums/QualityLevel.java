package com.coraybennett.spillway.service.enums;

public class QualityLevel {
    public final String name;
    public final int height;
    public final int width;
    public final String bitrate;
    public final String maxRate;
    public final String bufSize;
    public final String audioBitrate;
    public final int bandwidth;
    
    QualityLevel(String name, int height, int width, String bitrate, String maxRate, String bufSize, 
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

    // Define all possible quality levels
    public static final QualityLevel[] ALL_QUALITY_LEVELS = {
        new QualityLevel("2160p", 2160, 3840, "8000k", "8500k", "12000k", "192k", 8500000),
        new QualityLevel("1080p", 1080, 1920, "5000k", "5350k", "7500k", "192k", 5350000),
        new QualityLevel("720p", 720, 1280, "2500k", "2675k", "3750k", "128k", 2675000),
        new QualityLevel("480p", 480, 854, "1000k", "1075k", "1500k", "128k", 1075000),
        new QualityLevel("360p", 360, 640, "500k", "538k", "750k", "96k", 538000)
    };
}
