package com.coraybennett.spillway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enhanced asynchronous execution configuration with improved thread pool management
 * and configurable parameters for different workloads.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Value("${spring.task.execution.pool.core-size:4}")
    private int corePoolSize;
    
    @Value("${spring.task.execution.pool.max-size:8}")
    private int maxPoolSize;
    
    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${spring.task.execution.pool.keep-alive:60}")
    private int keepAliveSeconds;
    
    @Value("${video.conversion.thread-pool.core-size:2}")
    private int videoConversionCoreSize;
    
    @Value("${video.conversion.thread-pool.max-size:5}")
    private int videoConversionMaxSize;
    
    @Value("${video.conversion.thread-pool.queue-capacity:10}")
    private int videoConversionQueueCapacity;

    /**
     * Thread pool for video conversion tasks with optimized settings
     */
    @Bean(name = "videoConversionExecutor")
    public Executor videoConversionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(videoConversionCoreSize);
        executor.setMaxPoolSize(videoConversionMaxSize);
        executor.setQueueCapacity(videoConversionQueueCapacity);
        executor.setThreadNamePrefix("VideoConversion-");
        
        // Use caller runs policy to ensure videos are processed even if queue is full
        // This adds back-pressure instead of dropping tasks
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Set a reasonable keep-alive time for additional threads
        executor.setKeepAliveSeconds(120);
        
        // Allow core threads to timeout if they're idle
        executor.setAllowCoreThreadTimeOut(true);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * General purpose thread pool for other async operations
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("SpillwayAsync-");
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}