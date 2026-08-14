package com.taskflow.backend.dto;

import com.taskflow.backend.entity.Role;

/**
 * Returned by both register and login. The password hash is never part of any
 * response shape.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        String username,
        String email,
        Role role) {
}
