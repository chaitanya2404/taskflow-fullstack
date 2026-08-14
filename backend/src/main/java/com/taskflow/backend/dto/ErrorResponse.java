package com.taskflow.backend.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    public ErrorResponse {
        // defensive copy; null stays null so it is omitted from JSON
        // (spring.jackson.default-property-inclusion=non_null)
        fieldErrors = (fieldErrors == null) ? null : List.copyOf(fieldErrors);
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path,
                         List<FieldErrorDetail> fieldErrors) {
        this(Instant.now(), status, error, message, path, fieldErrors);
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
