package com.coraybennett.spillway.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.Loggable.LogLevel;
import com.coraybennett.spillway.dto.AuthRequest;
import com.coraybennett.spillway.dto.AuthResponse;
import com.coraybennett.spillway.dto.MessageResponse;
import com.coraybennett.spillway.dto.RegistrationRequest;
import com.coraybennett.spillway.dto.RegistrationResponse;
import com.coraybennett.spillway.dto.ResendConfirmationRequest;
import com.coraybennett.spillway.service.api.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller handling authentication operations.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    /**
     * Authenticate a user and return JWT token.
     * Returns AuthResponse DTO on success.
     */
    @PostMapping("/login")
    @Loggable(level = LogLevel.INFO, entryMessage = "User login", includeParameters = true)
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) {
        try {
            AuthResponse response = authService.authenticate(authRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication failed for user {}: {}", authRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Register a new user.
     * Returns RegistrationResponse DTO on success.
     */
    @PostMapping("/register")
    @Loggable(level = LogLevel.INFO, entryMessage = "User registration", includeParameters = true)
    public ResponseEntity<RegistrationResponse> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest) {
        try {
            RegistrationResponse response = authService.register(
                registrationRequest.getUsername(), 
                registrationRequest.getPassword(),
                registrationRequest.getEmail()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Confirm user email address.
     * Returns MessageResponse DTO with confirmation status.
     */
    @GetMapping("/confirm")
    @Loggable(entryMessage = "Email confirmation", includeParameters = true)
    public ResponseEntity<MessageResponse> confirmEmail(@RequestParam String token) {
        try {
            boolean confirmed = authService.confirmEmail(token);
            if (confirmed) {
                return ResponseEntity.ok(
                    MessageResponse.success("Email confirmed successfully. You can now log in.")
                );
            } else {
                return ResponseEntity.badRequest().body(
                    MessageResponse.error("Invalid or expired confirmation token")
                );
            }
        } catch (Exception e) {
            log.error("Email confirmation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                MessageResponse.error("Confirmation failed")
            );
        }
    }
    
    /**
     * Resend email confirmation.
     * Returns MessageResponse DTO with status.
     */
    @PostMapping("/resend-confirmation")
    @Loggable(entryMessage = "Resend confirmation email", includeParameters = true)
    public ResponseEntity<MessageResponse> resendConfirmation(@Valid @RequestBody ResendConfirmationRequest request) {
        try {
            boolean sent = authService.resendConfirmationEmail(request.getEmail());
            if (sent) {
                return ResponseEntity.ok(
                    MessageResponse.success("Confirmation email sent")
                );
            } else {
                return ResponseEntity.badRequest().body(
                    MessageResponse.error("Unable to send confirmation email")
                );
            }
        } catch (Exception e) {
            log.error("Failed to resend confirmation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                MessageResponse.error("Failed to resend confirmation")
            );
        }
    }
}