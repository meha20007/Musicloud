package com.musicloud.config;

import com.musicloud.security.JwtAuthenticationFilter;
import com.musicloud.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration.
 *
 * SIMPLE EXPLANATION of what each rule does:
 *   - POST /api/auth/**         → Anyone can register or log in (no token needed)
 *   - GET  /api/songs/**        → Anyone can browse songs (no token needed)
 *   - POST/PUT/DELETE /api/songs/** → Only ADMIN role can manage songs
 *   - GET  /api/users           → Only ADMIN can view all users
 *   - Everything else           → Must be logged in (valid JWT required)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtProvider;

    public SecurityConfig(JwtTokenProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless REST APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Allow CORS (so Swing client on localhost can call the backend)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // No session storage — the JWT is stateless
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Route-level access rules
            .authorizeHttpRequests(auth -> auth
                // Public: login and register
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/admin/login").permitAll()

                // Public: anyone can read songs
                .requestMatchers(HttpMethod.GET, "/api/songs", "/api/songs/**").permitAll()

                // Admin-only: song management
                .requestMatchers(HttpMethod.POST, "/api/songs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/songs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/songs/**").hasRole("ADMIN")

                // Admin-only: user list and delete
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Health check
                .requestMatchers("/actuator/health", "/api/health").permitAll()

                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )

            // Add our JWT filter BEFORE Spring's default auth filter
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Password encoder — BCrypt is the industry standard hashing algorithm.
     * Passwords are NEVER stored in plain text.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS allows the Java Swing desktop client (or any frontend on localhost)
     * to call this backend without being blocked by browser CORS policies.
     * We allow all origins for development; tighten this for production.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
