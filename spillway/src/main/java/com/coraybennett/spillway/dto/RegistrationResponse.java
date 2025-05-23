package com.coraybennett.spillway.dto;

public class RegistrationResponse {
    private String message;
    private boolean requiresEmailConfirmation;
    private String userId;
    
    public RegistrationResponse(String message, boolean requiresEmailConfirmation, String userId) {
        this.message = message;
        this.requiresEmailConfirmation = requiresEmailConfirmation;
        this.userId = userId;
    }
    
    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRequiresEmailConfirmation() { return requiresEmailConfirmation; }
    public void setRequiresEmailConfirmation(boolean requiresEmailConfirmation) { this.requiresEmailConfirmation = requiresEmailConfirmation; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}