package com.taskflow.backend.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forEntity(String entityName, Long id) {
        return new ResourceNotFoundException(entityName + " not found with id: " + id);
    }
}
