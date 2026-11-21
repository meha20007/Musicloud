package com.musicloud.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT creation and validation.
 *
 * Uses the SUPABASE_JWT_SECRET from your .env file when available.
 * If the secret is a placeholder (< 32 chars), falls back to a demo key
 * so the app still starts and you can test it without real Supabase credentials.
 *
 * SIMPLE EXPLANATION:
 *   - A JWT is like a signed ID card the server gives after login.
 *   - Every API request the Swing client makes carries this card.
 *   - The server checks the signature to know who is calling without
 *     hitting Supabase on every single request (fast!).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${supabase.jwt-secret:placeholder-jwt-secret-at-least-256-bits-long-use-a-real-one}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {

        this.expirationMs = expirationMs;

        // If the user has not yet set the real secret, use a fixed demo key
        // so the application starts and can be tested offline.
        byte[] keyBytes;
        if (secret == null || secret.startsWith("placeholder") || secret.length() < 32) {
            log.warn("[JWT] No real Supabase JWT secret found — using local demo key. " +
                     "Set SUPABASE_JWT_SECRET in your .env file when you have it.");
            keyBytes = "MusicLoudDemoSecretKey2026ForLocalDevOnly!".getBytes(StandardCharsets.UTF_8);
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Create a JWT token that encodes userId, username, and role */
    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /** Extract the user-id (subject) from a JWT */
    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /** Extract the username claim */
    public String getUsernameFromToken(String token) {
        return (String) getClaims(token).get("username");
    }

    /** Extract the role claim */
    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    /** Returns true if the token is valid and not expired */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Invalid token: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
