package com.coraybennett.spillway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that combines @RequiresAuthentication and @ResourceAccess for videos.
 * This shows how you can create specialized annotations for common security patterns.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RequiresAuthentication
@ResourceAccess(
    resourceType = ResourceAccess.ResourceType.VIDEO,
    handling = ResourceAccess.ResourceHandling.INJECT_RESOLVED
)
public @interface SecuredVideoResource {
    /**
     * Whether the operation requires write permission (ownership)
     */
    boolean requireWrite() default false;
}