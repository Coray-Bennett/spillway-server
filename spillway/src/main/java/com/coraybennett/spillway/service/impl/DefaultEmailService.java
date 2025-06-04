package com.coraybennett.spillway.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.coraybennett.spillway.service.api.EmailService;

import jakarta.mail.internet.MimeMessage;

@Service
@Profile("!dev")
public class DefaultEmailService implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultEmailService.class);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.from:noreply@spillway.com}")
    private String fromAddress;
    
    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;
    
    @Autowired
    public DefaultEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    @Override
    public boolean sendConfirmationEmail(String to, String username, String confirmationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Confirm your Spillway account");
            
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("confirmationLink", baseUrl + "/auth/confirm?token=" + confirmationToken);
            
            String htmlContent = templateEngine.process("confirmation-email", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Confirmation email sent to {}", to);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send confirmation email to {}", to, e);
            return false;
        }
    }
    
    @Override
    public boolean sendPasswordResetEmail(String to, String username, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Reset your Spillway password");
            
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetLink", baseUrl + "/auth/reset-password?token=" + resetToken);
            
            String htmlContent = templateEngine.process("password-reset-email", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Password reset email sent to {}", to);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", to, e);
            return false;
        }
    }
}