package com.coraybennett.spillway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.coraybennett.spillway.dto.AuthRequest;
import com.coraybennett.spillway.dto.AuthResponse;
import com.coraybennett.spillway.dto.RegistrationRequest;
import com.coraybennett.spillway.dto.RegistrationResponse;
import com.coraybennett.spillway.service.api.AuthService;

/**
 * Controller handling authentication operations.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            AuthResponse response = authService.authenticate(authRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest registrationRequest) {
        try {
            RegistrationResponse response = authService.register(
                registrationRequest.getUsername(), 
                registrationRequest.getPassword(),
                registrationRequest.getEmail()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/confirm")
    public ResponseEntity<?> confirmEmail(@RequestParam String token) {
        try {
            boolean confirmed = authService.confirmEmail(token);
            if (confirmed) {
                return ResponseEntity.ok("Email confirmed successfully. You can now log in.");
            } else {
                return ResponseEntity.badRequest().body("Invalid or expired confirmation token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Confirmation failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/resend-confirmation")
    public ResponseEntity<?> resendConfirmation(@RequestBody ResendConfirmationRequest request) {
        try {
            boolean sent = authService.resendConfirmationEmail(request.getEmail());
            if (sent) {
                return ResponseEntity.ok("Confirmation email sent");
            } else {
                return ResponseEntity.badRequest().body("Unable to send confirmation email");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to resend confirmation: " + e.getMessage());
        }
    }
    
    // DTO class for resend confirmation request
    public static class ResendConfirmationRequest {
        private String email;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}