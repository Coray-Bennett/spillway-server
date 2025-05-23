package com.coraybennett.spillway.service.api;

/**
 * Interface for email operations.
 */
public interface EmailService {
    
    /**
     * Sends an email confirmation link to a user.
     * 
     * @param to The recipient email address
     * @param username The username of the recipient
     * @param confirmationToken The confirmation token
     * @return true if email was sent successfully
     */
    boolean sendConfirmationEmail(String to, String username, String confirmationToken);
    
    /**
     * Sends a password reset email.
     * 
     * @param to The recipient email address
     * @param username The username of the recipient
     * @param resetToken The password reset token
     * @return true if email was sent successfully
     */
    boolean sendPasswordResetEmail(String to, String username, String resetToken);
}