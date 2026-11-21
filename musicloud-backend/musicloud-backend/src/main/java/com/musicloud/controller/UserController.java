package com.musicloud.controller;

import com.musicloud.model.UserEntity;
import com.musicloud.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User management REST API — ADMIN ONLY.
 *
 * GET    /api/users              → List all users
 * DELETE /api/users/{usernameOrId} → Delete a user account
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<Map<String, String>> users = userService.getAllUsers().stream()
                .map(u -> Map.of(
                        "id", u.getId(),
                        "username", u.getUsername() != null ? u.getUsername() : "",
                        "fullName", u.getFullName(),
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "role", u.getRole().name()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{usernameOrId}")
    public ResponseEntity<?> deleteUser(@PathVariable String usernameOrId) {
        try {
            userService.deleteUser(usernameOrId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
