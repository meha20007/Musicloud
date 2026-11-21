package com.musicloud.model;

import jakarta.persistence.Column;

/**
 * Extend UserEntity to hold the hashed password.
 * We keep it in the same table but separate it here for clarity.
 *
 * Since we cannot add a field to UserEntity without breaking it,
 * we simply add the password column directly on UserEntity.
 * This file just documents the design intent.
 *
 * NOTE: This file is intentionally empty — the @Column password
 * field has been placed directly in UserEntity.java for simplicity.
 */
public final class PasswordHolder {
    private PasswordHolder() {}
}
