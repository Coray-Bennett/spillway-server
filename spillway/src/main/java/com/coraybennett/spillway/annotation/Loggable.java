package com.coraybennett.spillway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method for automatic logging with standard patterns.
 * This annotation allows for consistent logging behavior without repetitive log statements.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    
    /**
     * The log level to use for entry and exit logs.
     */
    LogLevel level() default LogLevel.DEBUG;
    
    /**
     * Whether to include parameter values in the log.
     */
    boolean includeParameters() default false;
    
    /**
     * Whether to include return value in the log.
     */
    boolean includeResult() default false;
    
    /**
     * Whether to log exceptions.
     */
    boolean logExceptions() default true;
    
    /**
     * Custom entry message (optional). If not specified, a standard message is used.
     */
    String entryMessage() default "";
    
    /**
     * Custom exit message (optional). If not specified, a standard message is used.
     */
    String exitMessage() default "";
    
    /**
     * Available log levels.
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}