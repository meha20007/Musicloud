package com.musicloud.service;

import com.musicloud.dto.AuthResponse;
import com.musicloud.dto.LoginRequest;
import com.musicloud.dto.RegisterRequest;
import com.musicloud.model.UserEntity;
import com.musicloud.repository.UserRepository;
import com.musicloud.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication Service — handles registration and login.
 *
 * SIMPLE EXPLANATION:
 *   - Register: saves the user to our PostgreSQL database with a hashed password,
 *     then returns a JWT token so the user is immediately logged in.
 *   - Login: looks up the user, checks the password hash, and returns a fresh JWT.
 *
 * When you have your Supabase credentials, this service will ALSO create
 * the user in Supabase Auth so the email/password works from any device.
 * For now, it works entirely with our own PostgreSQL table.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    /**
     * Register a new user account.
     * Throws RuntimeException if email or username is already taken.
     */
    public AuthResponse register(RegisterRequest req) {
        // 1. Check for duplicates
        if (userRepository.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (userRepository.existsByUsername(req.getUsername().trim().toLowerCase())) {
            throw new IllegalArgumentException("That username is already taken. Please choose another.");
        }

        // 2. Build and save the user (password is hashed — never stored plain)
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setUsername(req.getUsername().trim().toLowerCase());
        user.setFirstName(capitalize(req.getFirstName().trim()));
        user.setLastName(capitalize(req.getLastName().trim()));
        user.setRole(UserEntity.Role.USER);
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        userRepository.save(user);
        log.info("[Auth] New user registered: {} ({})", user.getUsername(), user.getEmail());

        // 3. Issue a JWT and return it with user details
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return buildResponse(user, token);
    }

    /**
     * Login with username/email + password.
     * Returns a JWT token and user details.
     */
    public AuthResponse login(LoginRequest req) {
        String input = req.getUsernameOrEmail().trim().toLowerCase();

        // Try email first, then username
        UserEntity user = userRepository.findByEmail(input)
                .or(() -> userRepository.findByUsername(input))
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email or username."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password. Please try again.");
        }

        log.info("[Auth] User logged in: {} (role={})", user.getUsername(), user.getRole());

        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return buildResponse(user, token);
    }

    private AuthResponse buildResponse(UserEntity user, String token) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                86400000L // 24 hours in ms
        );
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
