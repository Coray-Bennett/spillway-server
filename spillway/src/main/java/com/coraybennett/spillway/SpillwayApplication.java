package com.coraybennett.spillway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpillwayApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpillwayApplication.class, args);
    }
}