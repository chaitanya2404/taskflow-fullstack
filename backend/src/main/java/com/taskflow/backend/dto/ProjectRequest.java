package com.taskflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(

        @NotBlank(message = "Project name is required")
        @Size(max = 120, message = "Project name must be at most 120 characters")
        String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description) {
}
