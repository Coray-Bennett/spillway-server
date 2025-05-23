package com.coraybennett.spillway.service.api;

import com.coraybennett.spillway.dto.AuthRequest;
import com.coraybennett.spillway.dto.AuthResponse;
import com.coraybennett.spillway.dto.RegistrationResponse;
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
     * @return Registration response with confirmation requirements
     * @throws IllegalArgumentException if validation fails
     * @throws Exception if registration fails
     */
    RegistrationResponse register(String username, String password, String email) throws Exception;
    
    /**
     * Confirms a user's email address.
     * 
     * @param token The confirmation token
     * @return true if confirmation was successful
     */
    boolean confirmEmail(String token);
    
    /**
     * Resends confirmation email.
     * 
     * @param email The user's email address
     * @return true if email was sent successfully
     */
    boolean resendConfirmationEmail(String email);
    
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