package com.musicloud.security;

/**
 * A simple object that holds the currently logged-in user's details.
 * Spring Security stores this in the SecurityContext after JWT is verified.
 */
public class MusicLoudPrincipal {

    private final String userId;   // Supabase UID / our internal user ID
    private final String username;

    public MusicLoudPrincipal(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }

    @Override
    public String toString() {
        return "MusicLoudPrincipal{userId='" + userId + "', username='" + username + "'}";
    }
}
