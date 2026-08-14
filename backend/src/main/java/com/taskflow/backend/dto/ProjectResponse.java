package com.taskflow.backend.dto;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        int taskCount) {
}
