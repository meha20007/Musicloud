package com.musicloud.dto;

/** Returned to Swing client after successful login */
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private long expiresIn;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String userId, String username,
                        String fullName, String email, String role, long expiresIn) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    // Getters & Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
