package com.taskflow.backend.controller;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.entity.TaskStatus;
import com.taskflow.backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "CRUD and filtering operations for tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List tasks, optionally filtered by projectId and/or status")
    public List<TaskResponse> getAll(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status) {

        if (projectId != null && status != null) {
            return taskService.findByProjectAndStatus(projectId, status);
        }
        if (projectId != null) {
            return taskService.findByProject(projectId);
        }
        if (status != null) {
            return taskService.findByStatus(status);
        }
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by id")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing task")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
