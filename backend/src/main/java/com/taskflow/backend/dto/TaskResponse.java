package com.taskflow.backend.dto;

import com.taskflow.backend.entity.TaskPriority;
import com.taskflow.backend.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Instant createdAt,
        Long projectId,
        String projectName) {
}
