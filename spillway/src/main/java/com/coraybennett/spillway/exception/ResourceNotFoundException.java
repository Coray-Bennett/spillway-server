package com.coraybennett.spillway.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceType, String id) {
        super(resourceType + " with ID '" + id + "' not found.");
    }
}