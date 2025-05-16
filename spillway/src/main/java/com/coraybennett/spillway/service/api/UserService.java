package com.coraybennett.spillway.service.api;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.coraybennett.spillway.model.User;

/**
 * Interface defining user management operations for the application.
 */
public interface UserService {
    
    /**
     * Loads a user by username for authentication purposes.
     * 
     * @param username The username to search for
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if the user is not found
     */
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    
    /**
     * Finds a user by their username.
     * 
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Finds a user by their ID.
     * 
     * @param id The user ID to search for
     * @return Optional containing the user if found
     */
    Optional<User> findById(String id);
    
    /**
     * Creates a new user.
     * 
     * @param user The user to create
     * @return The created user
     */
    User createUser(User user);
    
    /**
     * Updates an existing user.
     * 
     * @param user The user to update
     * @return The updated user
     */
    User updateUser(User user);
}