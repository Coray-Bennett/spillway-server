package com.coraybennett.spillway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.coraybennett.spillway.service.api.AuthService;
import com.coraybennett.spillway.service.api.PlaylistService;
import com.coraybennett.spillway.service.api.StorageService;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoConversionService;
import com.coraybennett.spillway.service.api.VideoService;
import com.coraybennett.spillway.service.impl.DefaultAuthService;
import com.coraybennett.spillway.service.impl.DefaultPlaylistService;
import com.coraybennett.spillway.service.impl.DefaultUserService;
import com.coraybennett.spillway.service.impl.DefaultVideoService;
import com.coraybennett.spillway.service.impl.FFmpegVideoConversionService;
import com.coraybennett.spillway.service.impl.FileSystemStorageService;

/**
 * Configuration class for application services.
 * This demonstrates how to configure different service implementations.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Example of providing a primary implementation for StorageService.
     * This allows for easy swapping of implementations.
     */
    @Bean
    @Primary
    public StorageService storageService() {
        return new FileSystemStorageService();
    }

    /**
     * Example of providing a specific video conversion implementation.
     * This could be swapped with other implementations (e.g., cloud-based) as needed.
     */
    @Bean
    @Primary
    public VideoConversionService videoConversionService() {
        return null; // The actual bean is created via component scanning
    }
}