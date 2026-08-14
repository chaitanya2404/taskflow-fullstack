package com.taskflow.backend.dto;

import java.time.Instant;

public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private int taskCount;

    public ProjectResponse() {
    }

    public ProjectResponse(Long id, String name, String description, Instant createdAt, int taskCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.taskCount = taskCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }
}
