package com.musicloud.service;

import com.musicloud.model.UserEntity;
import com.musicloud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user management (Admin-only operations).
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Get all registered users (Admin only) */
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    /** Delete a user by ID or username (Admin only) */
    @Transactional
    public void deleteUser(String usernameOrId) {
        // Try by ID first, then by username
        userRepository.findById(usernameOrId)
                .or(() -> userRepository.findByUsername(usernameOrId))
                .ifPresentOrElse(
                        userRepository::delete,
                        () -> { throw new IllegalArgumentException("User not found: " + usernameOrId); }
                );
    }
}
