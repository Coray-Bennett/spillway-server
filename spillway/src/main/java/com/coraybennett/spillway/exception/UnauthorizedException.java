package com.coraybennett.spillway.exception;

/**
 * Exception thrown when an unauthenticated user attempts to access a resource that requires authentication.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException() {
        super("Authentication required to access this resource.");
    }
}