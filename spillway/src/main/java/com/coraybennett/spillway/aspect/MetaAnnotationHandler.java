package com.coraybennett.spillway.aspect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.annotation.ResolvedResource;
import com.coraybennett.spillway.annotation.SecuredPlaylistResource;
import com.coraybennett.spillway.annotation.SecuredVideoResource;
import com.coraybennett.spillway.annotation.UserAction;
import com.coraybennett.spillway.model.Playlist;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.model.Video;
import com.coraybennett.spillway.service.api.PlaylistService;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.service.api.VideoAccessService;
import com.coraybennett.spillway.service.api.VideoService;

/**
 * Aspect to handle meta-annotations for security checks.
 */
@Aspect
@Component
@Order(1) // Ensure this runs first, before other aspects
public class MetaAnnotationHandler {

    private final UserService userService;
    private final VideoService videoService;
    private final PlaylistService playlistService;
    private final VideoAccessService videoAccessService;

    @Autowired
    public MetaAnnotationHandler(
        UserService userService, 
        VideoService videoService,
        PlaylistService playlistService,
        VideoAccessService videoAccessService) {
        this.userService = userService;
        this.videoService = videoService;
        this.playlistService = playlistService;
        this.videoAccessService = videoAccessService;
    }

    /**
     * Processes the @UserAction annotation.
     */
    @Around("@annotation(userAction)")
    public Object processUserAction(ProceedingJoinPoint joinPoint, UserAction userAction) throws Throwable {
        // Check authentication if required
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAuthenticated = authentication != null && 
                                authentication.isAuthenticated() && 
                                !"anonymousUser".equals(authentication.getName());

        // If authentication is required but user is not authenticated
        if (userAction.requireAuth() && !isAuthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // If authenticated, inject user
        if (isAuthenticated) {
            User user = userService.findByUsername(authentication.getName())
                .orElse(null);
                
            if (user == null && userAction.requireAuth()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Replace @CurrentUser and Principal parameters
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?>[] paramTypes = method.getParameterTypes();
            
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] == User.class) {
                    Parameter param = method.getParameters()[i];
                    CurrentUser annotation = param.getAnnotation(CurrentUser.class);
                    if (annotation != null) {
                        args[i] = user;
                    }
                } else if (paramTypes[i] == Principal.class && args[i] == null) {
                    // Fallback for Principal - shouldn't be needed with proper refactoring
                    args[i] = authentication;
                }
            }
            
            return joinPoint.proceed(args);
        } else {
            // Unauthenticated but authentication is optional
            return joinPoint.proceed();
        }
    }

    /**
     * Processes the @SecuredVideoResource annotation.
     */
    @Around("@annotation(securedVideo)")
    public Object processSecuredVideoResource(ProceedingJoinPoint joinPoint, SecuredVideoResource securedVideo) 
            throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        
        // Extract user from authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && 
                                authentication.isAuthenticated() && 
                                !"anonymousUser".equals(authentication.getName());
                                
        User user = null;
        if (isAuthenticated) {
            user = userService.findByUsername(authentication.getName()).orElse(null);
        }
        
        // If authentication is required but user is not authenticated
        if (!securedVideo.optionalAuth() && (user == null)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String videoId = null;
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            PathVariable pathVar = param.getAnnotation(PathVariable.class);
            
            if (pathVar != null && (securedVideo.idParameter().equals(pathVar.value()) || securedVideo.idParameter().equals(pathVar.name()))) {
                if (args[i] != null) {
                    videoId = args[i].toString();
                    break;
                }
            }
        }
        
        if (videoId == null) {
            return ResponseEntity.badRequest().body("Video ID not found");
        }
        
        // Fetch the video
        Optional<Video> videoOpt = videoService.getVideoById(videoId);
        if (videoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Video video = videoOpt.get();
        
        // Check permissions
        boolean hasAccess = false;
        if (securedVideo.requireWrite()) {
            // Write access requires ownership
            hasAccess = user != null && 
                      video.getUploadedBy() != null &&
                      video.getUploadedBy().getId().equals(user.getId());
        } else {
            // Read access can be more permissive
            hasAccess = videoAccessService.canAccessVideo(video, user);
        }
        
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // If we need to inject the video, find the right parameter and inject it
        if (securedVideo.handling() == SecuredVideoResource.ResourceHandling.INJECT_RESOLVED) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(ResolvedResource.class)) {
                    args[i] = video;
                }
            }
        }
        
        // Inject user if parameter is present
        if (user != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getType() == User.class && 
                    parameters[i].isAnnotationPresent(CurrentUser.class)) {
                    args[i] = user;
                }
            }
        }
        
        return joinPoint.proceed(args);
    }
    
    /**
     * Processes the @SecuredPlaylistResource annotation.
     */
    @Around("@annotation(securedPlaylist)")
    public Object processSecuredPlaylistResource(ProceedingJoinPoint joinPoint, SecuredPlaylistResource securedPlaylist) 
            throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        
        // Extract user from authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && 
                                authentication.isAuthenticated() && 
                                !"anonymousUser".equals(authentication.getName());
                                
        User user = null;
        if (isAuthenticated) {
            user = userService.findByUsername(authentication.getName()).orElse(null);
        }
        
        // If authentication is required but user is not authenticated
        if (!securedPlaylist.optionalAuth() && (user == null)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Find the playlist ID parameter
        String idParamName = securedPlaylist.idParameter();
        String playlistId = null;
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            PathVariable pathVar = param.getAnnotation(PathVariable.class);
            
            if (pathVar != null && (idParamName.equals(pathVar.value()) || idParamName.equals(pathVar.name()))) {
                if (args[i] != null) {
                    playlistId = args[i].toString();
                    break;
                }
            }
        }
        
        if (playlistId == null) {
            return ResponseEntity.badRequest().body("Playlist ID not found");
        }
        
        // Fetch the playlist
        Optional<Playlist> playlistOpt = playlistService.getPlaylistById(playlistId);
        if (playlistOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Playlist playlist = playlistOpt.get();
        
        // Check permissions
        boolean hasAccess = false;
        if (securedPlaylist.requireWrite()) {
            // Write access requires ownership
            hasAccess = user != null && 
                      playlist.getCreatedBy() != null &&
                      playlist.getCreatedBy().getId().equals(user.getId());
        } else {
            // Read access can be more permissive
            hasAccess = videoAccessService.canAccessPlaylist(playlist, user);
        }
        
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // If we need to inject the playlist, find the right parameter and inject it
        if (securedPlaylist.handling() == SecuredPlaylistResource.ResourceHandling.INJECT_RESOLVED) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(ResolvedResource.class)) {
                    args[i] = playlist;
                }
            }
        }
        
        // Inject user if parameter is present
        if (user != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getType() == User.class && 
                    parameters[i].isAnnotationPresent(CurrentUser.class)) {
                    args[i] = user;
                }
            }
        }
        
        return joinPoint.proceed(args);
    }
}