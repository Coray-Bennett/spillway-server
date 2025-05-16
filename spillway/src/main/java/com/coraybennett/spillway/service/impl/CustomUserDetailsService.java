package com.coraybennett.spillway.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.repository.UserRepository;
import com.coraybennett.spillway.service.api.UserService;

/**
 * Implements both Spring Security's UserDetailsService and our custom UserService.
 */
@Service
@Primary
public class CustomUserDetailsService implements UserService, UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(username);
        builder.password(user.getPassword());
        builder.authorities("USER");
        return builder.build();
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
    
    @Override
    public User createUser(User user) {
        if (user.getId() != null && userRepository.existsById(user.getId())) {
            throw new IllegalArgumentException("Cannot create user with existing ID");
        }
        return userRepository.save(user);
    }
    
    @Override
    public User updateUser(User user) {
        if (!userRepository.existsById(user.getId())) {
            throw new IllegalArgumentException("Cannot update non-existent user: " + user.getId());
        }
        return userRepository.save(user);
    }
}
