package com.coraybennett.spillway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableAsync
@Slf4j
public class SpillwayApplication {
    public static void main(String[] args) {
        log.info("Starting Spillway Video Streaming Application...");
        SpringApplication.run(SpillwayApplication.class, args);
        log.info("Spillway application started successfully");
    }
}