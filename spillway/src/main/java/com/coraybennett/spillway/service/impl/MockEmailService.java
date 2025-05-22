package com.coraybennett.spillway.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.coraybennett.spillway.service.api.EmailService;

@Service
@Profile("dev") // Only active in dev profile
public class MockEmailService implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);
    
    @Override
    public boolean sendConfirmationEmail(String to, String username, String confirmationToken) {
        logger.info("DEV MODE: Email confirmation disabled. Token for {}: {}", username, confirmationToken);
        logger.info("DEV MODE: Confirmation link would be: http://localhost:8081/auth/confirm?token={}", confirmationToken);
        return true;
    }
    
    @Override
    public boolean sendPasswordResetEmail(String to, String username, String resetToken) {
        logger.info("DEV MODE: Password reset email disabled. Token for {}: {}", username, resetToken);
        logger.info("DEV MODE: Reset link would be: http://localhost:8081/auth/reset-password?token={}", resetToken);
        return true;
    }
}