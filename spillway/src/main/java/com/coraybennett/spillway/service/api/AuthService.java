package com.coraybennett.spillway.service.api;

import com.coraybennett.spillway.dto.AuthRequest;
import com.coraybennett.spillway.dto.AuthResponse;
import com.coraybennett.spillway.model.User;

/**
 * Interface defining authentication operations.
 */
public interface AuthService {
    
    /**
     * Authenticates a user and generates a token.
     * 
     * @param authRequest The authentication credentials
     * @return AuthResponse containing the token and user information
     * @throws Exception if authentication fails
     */
    AuthResponse authenticate(AuthRequest authRequest) throws Exception;
    
    /**
     * Creates a new user account (registration).
     * 
     * @param username Username
     * @param password Password
     * @param email Email address
     * @return The created user
     * @throws Exception if registration fails
     */
    User register(String username, String password, String email) throws Exception;
    
    /**
     * Validates a token.
     * 
     * @param token The authentication token
     * @return true if the token is valid, false otherwise
     */
    boolean validateToken(String token);
    
    /**
     * Gets the user associated with a token.
     * 
     * @param token The authentication token
     * @return The user or null if token is invalid
     */
    User getUserFromToken(String token);
}