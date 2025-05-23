package com.coraybennett.spillway.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.coraybennett.spillway.annotation.ResourceAccess;
import com.coraybennett.spillway.annotation.SecuredVideoResource;

/**
 * Aspect to handle meta-annotations.
 * This example shows how to process SecuredVideoResource meta-annotation.
 */
@Aspect
@Component
public class MetaAnnotationHandler {

    /**
     * Processes the @SecuredVideoResource annotation and maps it to the underlying annotations.
     */
    @Around("@annotation(com.coraybennett.spillway.annotation.SecuredVideoResource)")
    public Object processSecuredVideoResource(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Get the meta-annotation
        SecuredVideoResource metaAnnotation = method.getAnnotation(SecuredVideoResource.class);
        
        // Extract parameters from meta-annotation
        boolean requireWrite = metaAnnotation.requireWrite();
        
        // Manual handling (in practice, you would likely create a ResourceAccess annotation here
        // programmatically or use reflection to modify the existing one if possible)
        
        return joinPoint.proceed();
    }
}