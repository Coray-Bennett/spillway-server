package com.coraybennett.spillway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for methods that operate on the authenticated user.
 * This is processed by a dedicated aspect that will apply user authentication.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserAction {
    /**
     * Whether to require authentication or make it optional.
     * When true, unauthenticated users will receive UNAUTHORIZED.
     */
    boolean requireAuth() default true;
}