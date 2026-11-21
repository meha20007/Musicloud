package com.musicloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables the @PreAuthorize and @PostAuthorize annotations used in controllers.
 * Without this, @PreAuthorize("hasRole('ADMIN')") would do nothing.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
}
