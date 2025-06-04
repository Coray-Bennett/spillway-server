package com.coraybennett.spillway.aspect;

import com.coraybennett.spillway.annotation.CurrentUser;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.service.api.UserService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Aspect to handle authentication requirements and user injection.
 */
@Aspect
@Component
public class AuthenticationAspect {

    private final UserService userService;

    @Autowired
    public AuthenticationAspect(UserService userService) {
        this.userService = userService;
    }

    @Around("@annotation(com.coraybennett.spillway.annotation.RequiresAuthentication)")
    public Object handleAuthentication(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        Optional<User> userOpt = userService.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        
        // Replace @CurrentUser parameters with the actual user
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation instanceof CurrentUser) {
                    args[i] = user;
                    break;
                }
            }
        }
        
        return joinPoint.proceed(args);
    }
}