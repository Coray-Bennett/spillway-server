package com.coraybennett.spillway.aspect;

import com.coraybennett.spillway.annotation.ResourceAccess;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.PlaylistService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Aspect to handle resource access verification and injection.
 * This aspect should run after the AuthenticationAspect to ensure the user is available.
 */
@Aspect
@Component
@Order(2) // Run after AuthenticationAspect
public class ResourceAccessAspect {

    private final VideoService videoService;
    private final PlaylistService playlistService;
    private final VideoAccessService videoAccessService;

    @Autowired
    public ResourceAccessAspect(
            VideoService videoService,
            PlaylistService playlistService,
            VideoAccessService videoAccessService) {
        this.videoService = videoService;
        this.playlistService = playlistService;
        this.videoAccessService = videoAccessService;
    }

    @Around("@annotation(resourceAccess)")
    public Object handleResourceAccess(ProceedingJoinPoint joinPoint, ResourceAccess resourceAccess) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        String idParamName = resourceAccess.idParameter();
        
        String resourceId = null;
        User currentUser = null;
        
        // Extract parameters we need
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            // Find the parameter with the ID
            Parameter param = parameters[i];
            String paramName = param.getName();
            
            // Get resource ID from path variable, request param or directly by parameter name
            if (paramName.equals(idParamName) || paramHasAnnotation(param, idParamName, PathVariable.class) 
                    || paramHasAnnotation(param, idParamName, RequestParam.class)) {
                if (args[i] != null) {
                    resourceId = args[i].toString();
                }
            }
            
            // Find the current user
            if (args[i] instanceof User) {
                currentUser = (User) args[i];
            }
        }
        
        if (resourceId == null) {
            return ResponseEntity.badRequest().body("Resource ID not found");
        }
        
        // Verify resource access based on resource type
        Optional<?> resourceOpt;
        boolean hasAccess = false;
        Object resource = null;
        
        switch (resourceAccess.resourceType()) {
            case VIDEO:
                resourceOpt = videoService.getVideoById(resourceId);
                if (resourceOpt.isPresent()) {
                    Video video = (Video) resourceOpt.get();
                    resource = video;
                    
                    if (resourceAccess.requireWriteAccess()) {
                        hasAccess = currentUser != null && 
                                    video.getUploadedBy() != null && 
                                    video.getUploadedBy().getId().equals(currentUser.getId());
                    } else {
                        hasAccess = videoAccessService.canAccessVideo(video, currentUser);
                    }
                }
                break;
                
            case PLAYLIST:
                resourceOpt = playlistService.getPlaylistById(resourceId);
                if (resourceOpt.isPresent()) {
                    Playlist playlist = (Playlist) resourceOpt.get();
                    resource = playlist;
                    
                    if (resourceAccess.requireWriteAccess()) {
                        hasAccess = currentUser != null && 
                                    playlist.getCreatedBy() != null && 
                                    playlist.getCreatedBy().getId().equals(currentUser.getId());
                    } else {
                        hasAccess = videoAccessService.canAccessPlaylist(playlist, currentUser);
                    }
                }
                break;
                
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Unsupported resource type");
        }
        
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // If we need to inject the resource, find the right parameter and inject it
        if (resourceAccess.handling() == ResourceAccess.ResourceHandling.INJECT_RESOLVED) {
            for (int i = 0; i < parameters.length; i++) {
                Annotation[] paramAnnotations = parameters[i].getAnnotations();
                for (Annotation annotation : paramAnnotations) {
                    if (annotation instanceof ResolvedResource) {
                        args[i] = resource;
                        break;
                    }
                }
            }
        }
        
        return joinPoint.proceed(args);
    }
    
    private boolean paramHasAnnotation(Parameter param, String target, Class<? extends Annotation> annotationType) {
        for (Annotation annotation : param.getAnnotations()) {
            if (annotation.annotationType() == annotationType) {
                try {
                    Method valueMethod = annotationType.getDeclaredMethod("value");
                    String value = (String) valueMethod.invoke(annotation);
                    return value.equals(target);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }
}