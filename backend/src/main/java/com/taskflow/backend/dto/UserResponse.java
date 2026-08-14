package com.taskflow.backend.dto;

import com.taskflow.backend.entity.Role;

import java.time.Instant;

/** The currently authenticated account, as returned by {@code GET /api/auth/me}. */
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        Instant createdAt) {
}
