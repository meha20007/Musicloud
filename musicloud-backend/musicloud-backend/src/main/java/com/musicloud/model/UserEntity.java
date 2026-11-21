package com.musicloud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a registered MusicLoud user.
 * Maps to the "users" table in Supabase PostgreSQL.
 *
 * We store the Supabase Auth UID as the primary key so that
 * the backend can directly use the JWT subject (sub) claim.
 */
@Entity
@Table(name = "ml_users")
public class UserEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id; // Supabase Auth UUID (sub from JWT)

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role = Role.USER;

    @Column(name = "password_hash", length = 100)
    private String password;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (id == null) id = UUID.randomUUID().toString();
    }

    public enum Role { USER, ADMIN }

    // ----- Constructors -----
    public UserEntity() {}

    public UserEntity(String id, String email, String username,
                      String firstName, String lastName, Role role) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // ----- Getters / Setters -----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() {
        String f = (firstName != null ? firstName : "");
        String l = (lastName != null ? lastName : "");
        return (f + " " + l).trim();
    }
}
