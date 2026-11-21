package com.musicloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * This filter runs ONCE per HTTP request.
 *
 * SIMPLE EXPLANATION:
 *   Every time the Swing client calls an API, it sends the JWT in the
 *   "Authorization: Bearer <token>" header.
 *   This filter reads that token, verifies it, and tells Spring Security
 *   who the user is so that controllers can protect routes properly.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtProvider.validateToken(token)) {
                try {
                    String userId   = jwtProvider.getUserIdFromToken(token);
                    String username = jwtProvider.getUsernameFromToken(token);
                    String role     = jwtProvider.getRoleFromToken(token);

                    // e.g. "ADMIN" becomes "ROLE_ADMIN" for Spring Security
                    String springRole = "ROLE_" + (role != null ? role.toUpperCase() : "USER");

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    new MusicLoudPrincipal(userId, username),
                                    null,
                                    List.of(new SimpleGrantedAuthority(springRole))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("[JWT] Authenticated user: {} role: {}", username, role);
                } catch (Exception e) {
                    log.warn("[JWT] Could not set authentication: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
