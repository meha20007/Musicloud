package com.musicloud.config;

import com.musicloud.model.UserEntity;
import com.musicloud.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * Database initializer — runs once when the Spring Boot app starts.
 *
 * WHAT IT DOES:
 *   - Creates the default "admin" account if it doesn't already exist.
 *   - Admin username: admin   Password: admin  (same as your existing local app)
 *   - You can change the password in the .env file (ADMIN_PASSWORD variable).
 *
 * This keeps your existing admin/admin login working in the cloud version!
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedAdminUser(UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            if (!userRepo.existsByUsername("admin")) {
                UserEntity admin = new UserEntity();
                admin.setId(UUID.randomUUID().toString());
                admin.setEmail("admin@musicloud.local");
                admin.setUsername("admin");
                admin.setFirstName("Admin");
                admin.setLastName("User");
                admin.setRole(UserEntity.Role.ADMIN);
                // Default password is "admin" — same as your existing local login
                admin.setPassword(encoder.encode("admin"));
                userRepo.save(admin);
                log.info("[Init] Admin account created. Username: admin | Password: admin");
            } else {
                log.info("[Init] Admin account already exists.");
            }
        };
    }
}
