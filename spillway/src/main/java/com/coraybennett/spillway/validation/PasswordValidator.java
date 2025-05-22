package com.coraybennett.spillway.validation;

import org.passay.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PasswordValidator {
    
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>();
    
    static {
        // Load common passwords from resource file
        try (InputStream is = PasswordValidator.class.getResourceAsStream("/common-passwords.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        COMMON_PASSWORDS.add(line.trim().toLowerCase());
                    }
                }
            }
        } catch (IOException e) {
            // Log error but don't fail initialization
        }
    }
    
    private final org.passay.PasswordValidator validator;
    
    public PasswordValidator() {
        this.validator = new org.passay.PasswordValidator(Arrays.asList(
            // Length requirement: 12-128 characters
            new LengthRule(12, 128),
            
            // At least one uppercase letter
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            
            // At least one lowercase letter
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            
            // At least one digit
            new CharacterRule(EnglishCharacterData.Digit, 1),
            
            // At least one special character
            new CharacterRule(EnglishCharacterData.Special, 1),
            
            // No whitespace
            new WhitespaceRule(),
            
            // No sequences like 'abc' or '123'
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 3, false),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 3, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 3, false),
            
            // No repeated characters (e.g., 'aaa')
            new RepeatCharacterRegexRule(3)
        ));
    }
    
    public ValidationResult validate(String password, String username) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password cannot be empty");
        }
        
        // Check against common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            return new ValidationResult(false, "This password is too common. Please choose a more secure password.");
        }
        
        // Check if password contains username
        if (username != null && password.toLowerCase().contains(username.toLowerCase())) {
            return new ValidationResult(false, "Password cannot contain your username");
        }
        
        // Validate against Passay rules
        RuleResult result = validator.validate(new PasswordData(password));
        
        if (result.isValid()) {
            return new ValidationResult(true, null);
        } else {
            List<String> messages = validator.getMessages(result);
            return new ValidationResult(false, String.join(". ", messages));
        }
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}