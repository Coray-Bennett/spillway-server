package com.coraybennett.spillway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for securing playlist resources.
 * This is processed by a dedicated aspect that will apply the necessary security checks.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredPlaylistResource {
    /**
     * Whether the operation requires write permission (ownership)
     */
    boolean requireWrite() default false;
    
    /**
     * Whether authentication is optional.
     * If false, user must be authenticated.
     * If true, unauthenticated access may be permitted with limited privileges.
     */
    boolean optionalAuth() default false;
    
    /**
     * Specify how to handle the resolved resource
     */
    ResourceHandling handling() default ResourceHandling.INJECT_RESOLVED;
    
    /**
     * The parameter name that contains the playlist ID
     */
    String idParameter() default "id";
    
    /**
     * How to handle the resolved resource
     */
    enum ResourceHandling {
        VERIFY_ONLY,       // Just verify access, don't inject
        INJECT_RESOLVED    // Inject the resolved resource into a parameter
    }
}