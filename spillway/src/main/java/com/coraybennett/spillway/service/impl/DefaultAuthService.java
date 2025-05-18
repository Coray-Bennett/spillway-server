package com.coraybennett.spillway.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coraybennett.spillway.dto.AuthRequest;
import com.coraybennett.spillway.dto.AuthResponse;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.repository.UserRepository;
import com.coraybennett.spillway.security.JwtUtil;
import com.coraybennett.spillway.service.api.AuthService;
import com.coraybennett.spillway.service.api.UserService;

/**
 * Default implementation of the AuthService interface.
 */
@Service
public class DefaultAuthService implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DefaultAuthService(
            AuthenticationManager authenticationManager,
            UserService userService,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }

        final UserDetails userDetails = userService.loadUserByUsername(authRequest.getUsername());
        final String token = jwtUtil.generateToken(userDetails);
        
        return new AuthResponse(token);
    }

    @Override
    @Transactional
    public User register(String username, String password, String email) throws Exception {
        // Check if username already exists
        if (userService.findByUsername(username).isPresent()) {
            throw new Exception("Username already exists");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        
        return userRepository.save(user);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return jwtUtil.validateToken(token, userService.loadUserByUsername(jwtUtil.extractUsername(token)));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public User getUserFromToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            return userService.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}