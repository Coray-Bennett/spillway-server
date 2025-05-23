package com.coraybennett.spillway.validation;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UsernameValidator {
    
    // Username: 3-20 characters, alphanumeric and underscores only
    // Must start with a letter
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{2,19}$");
    
    public ValidationResult validate(String username) {
        if (username == null || username.isEmpty()) {
            return new ValidationResult(false, "Username cannot be empty");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new ValidationResult(false, 
                "Username must be 3-20 characters long, start with a letter, " +
                "and contain only letters, numbers, and underscores");
        }
        
        // Check for reserved usernames
        String lowerUsername = username.toLowerCase();
        if (isReservedUsername(lowerUsername)) {
            return new ValidationResult(false, "This username is reserved");
        }
        
        return new ValidationResult(true, null);
    }
    
    private boolean isReservedUsername(String username) {
        String[] reserved = {
            "admin", "administrator", "root", "system", "user", "guest",
            "api", "auth", "login", "logout", "register", "signup",
            "video", "playlist", "upload", "spillway", "test", "demo"
        };
        
        for (String r : reserved) {
            if (r.equals(username)) {
                return true;
            }
        }
        return false;
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