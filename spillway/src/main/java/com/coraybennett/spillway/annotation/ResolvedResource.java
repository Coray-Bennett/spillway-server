package com.coraybennett.spillway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter that should be populated with a resolved resource.
 * Used in conjunction with @ResourceAccess with handling set to INJECT_RESOLVED.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResolvedResource {
}