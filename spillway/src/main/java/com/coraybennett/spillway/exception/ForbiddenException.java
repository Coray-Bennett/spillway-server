package com.coraybennett.spillway.exception;

/**
 * Exception thrown when an authenticated user doesn't have sufficient permissions to access a resource.
 */
public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException() {
        super("You don't have permission to access this resource.");
    }
}