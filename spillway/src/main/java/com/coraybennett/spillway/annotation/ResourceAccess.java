package com.coraybennett.spillway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate resource access verification is needed.
 * Specifies the type of resource and which parameters contain the resource ID.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceAccess {
    
    /**
     * The resource type that is being accessed.
     */
    ResourceType resourceType();
    
    /**
     * The name of the parameter that contains the ID of the resource.
     */
    String idParameter() default "id";
    
    /**
     * Whether write access is required (owner-only) or just read access.
     */
    boolean requireWriteAccess() default false;
    
    /**
     * Specifies what to do with the resolved resource.
     */
    ResourceHandling handling() default ResourceHandling.VERIFY_ONLY;
    
    /**
     * Parameter name to store the resolved resource (when handling is INJECT_RESOLVED)
     */
    String resolveTo() default "";
    
    /**
     * Supported resource types
     */
    enum ResourceType {
        VIDEO,
        PLAYLIST
    }
    
    /**
     * How to handle the resource after resolving
     */
    enum ResourceHandling {
        VERIFY_ONLY,       // Just verify access, don't inject
        INJECT_RESOLVED    // Inject the resolved resource into a parameter
    }
}