package com.coraybennett.spillway;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("*");
    }
    
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/hls/**")
            .addResourceLocations("content/**")
            .setCacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS))
            .resourceChain(true)
            .addResolver(new PathResourceResolver());
    }

    @ControllerAdvice
    public class HLSControllerAdvice {
        @ModelAttribute
        public void setHeaders(HttpServletResponse response) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Range");
            response.setHeader("Access-Control-Expose-Headers", "Content-Range, Accept-Ranges");
        }
    }
}
