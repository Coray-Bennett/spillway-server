package com.coraybennett.spillway.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.coraybennett.spillway.dto.RegistrationResponse;
import com.coraybennett.spillway.model.User;
import com.coraybennett.spillway.repository.UserRepository;
import com.coraybennett.spillway.security.JwtUtil;
import com.coraybennett.spillway.service.api.AuthService;
import com.coraybennett.spillway.service.api.EmailService;
import com.coraybennett.spillway.service.api.UserService;
import com.coraybennett.spillway.validation.PasswordValidator;
import com.coraybennett.spillway.validation.UsernameValidator;

/**
 * Default implementation of the AuthService interface.
 */
@Service
public class DefaultAuthService implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthService.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UsernameValidator usernameValidator;
    private final PasswordValidator passwordValidator;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
    @Value("${auth.confirmation.token.expiry-hours:24}")
    private int confirmationTokenExpiryHours;

    @Autowired
    public DefaultAuthService(
            AuthenticationManager authenticationManager,
            UserService userService,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            UsernameValidator usernameValidator,
            PasswordValidator passwordValidator) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.usernameValidator = usernameValidator;
        this.passwordValidator = passwordValidator;
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) throws Exception {
        try {
            // First check if user exists and is enabled
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            
            if (!user.isEnabled()) {
                if (!user.isEmailConfirmed()) {
                    throw new DisabledException("Please confirm your email before logging in");
                }
                throw new DisabledException("Your account has been disabled");
            }
            
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()));
        } catch (DisabledException e) {
            throw new Exception(e.getMessage(), e);
        } catch (BadCredentialsException e) {
            throw new Exception("Invalid username or password", e);
        }

        final UserDetails userDetails = userService.loadUserByUsername(authRequest.getUsername());
        final String token = jwtUtil.generateToken(userDetails);
        
        return new AuthResponse(token);
    }

    @Override
    @Transactional
    public RegistrationResponse register(String username, String password, String email) throws Exception {
        // Validate username
        UsernameValidator.ValidationResult usernameResult = usernameValidator.validate(username);
        if (!usernameResult.isValid()) {
            throw new IllegalArgumentException(usernameResult.getErrorMessage());
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Validate email
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Validate password
        PasswordValidator.ValidationResult passwordResult = passwordValidator.validate(password, username);
        if (!passwordResult.isValid()) {
            throw new IllegalArgumentException(passwordResult.getErrorMessage());
        }
        
        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        
        boolean requiresEmailConfirmation = !"dev".equals(activeProfile);
        
        if (requiresEmailConfirmation) {
            // Generate confirmation token
            String confirmationToken = UUID.randomUUID().toString();
            user.setConfirmationToken(confirmationToken);
            user.setConfirmationTokenExpiry(LocalDateTime.now().plusHours(confirmationTokenExpiryHours));
            user.setEmailConfirmed(false);
            user.setEnabled(false);
        } else {
            // In dev mode, auto-confirm
            user.setEmailConfirmed(true);
            user.setEnabled(true);
            logger.info("DEV MODE: Auto-confirming user {}", username);
        }
        
        User savedUser = userRepository.save(user);
        
        if (requiresEmailConfirmation) {
            // Send confirmation email
            emailService.sendConfirmationEmail(email, username, savedUser.getConfirmationToken());
            
            return new RegistrationResponse(
                "Registration successful. Please check your email to confirm your account.",
                true,
                savedUser.getId()
            );
        } else {
            return new RegistrationResponse(
                "Registration successful. You can now log in.",
                false,
                savedUser.getId()
            );
        }
    }

    @Override
    @Transactional
    public boolean confirmEmail(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        User user = userRepository.findByConfirmationToken(token).orElse(null);
        if (user == null) {
            return false;
        }
        
        // Check if token is expired
        if (user.getConfirmationTokenExpiry() != null && 
            LocalDateTime.now().isAfter(user.getConfirmationTokenExpiry())) {
            return false;
        }
        
        // Confirm the email
        user.setEmailConfirmed(true);
        user.setEnabled(true);
        user.setConfirmationToken(null);
        user.setConfirmationTokenExpiry(null);
        userRepository.save(user);
        
        logger.info("Email confirmed for user: {}", user.getUsername());
        return true;
    }

    @Override
    @Transactional
    public boolean resendConfirmationEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.isEmailConfirmed()) {
            return false;
        }
        
        // Generate new token
        String newToken = UUID.randomUUID().toString();
        user.setConfirmationToken(newToken);
        user.setConfirmationTokenExpiry(LocalDateTime.now().plusHours(confirmationTokenExpiryHours));
        userRepository.save(user);
        
        // Send email
        return emailService.sendConfirmationEmail(email, user.getUsername(), newToken);
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