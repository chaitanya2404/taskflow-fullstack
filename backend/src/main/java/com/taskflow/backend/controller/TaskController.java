package com.taskflow.backend.controller;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.entity.TaskStatus;
import com.taskflow.backend.security.AuthenticatedUser;
import com.taskflow.backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Tasks", description = "CRUD and filtering for tasks in the authenticated user's projects")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List your tasks, optionally filtered by projectId and/or status")
    public List<TaskResponse> getAll(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        Long ownerId = principal.getId();

        if (projectId != null && status != null) {
            return taskService.findByProjectAndStatus(projectId, status, ownerId);
        }
        if (projectId != null) {
            return taskService.findByProject(projectId, ownerId);
        }
        if (status != null) {
            return taskService.findByStatus(status, ownerId);
        }
        return taskService.findAll(ownerId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of your tasks by id",
            description = "Returns 404 for ids that do not exist and for tasks in someone else's project.")
    public TaskResponse getById(@PathVariable Long id,
                                @AuthenticationPrincipal AuthenticatedUser principal) {
        return taskService.findById(id, principal.getId());
    }

    @PostMapping
    @Operation(summary = "Create a task in one of your projects")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        TaskResponse created = taskService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of your tasks")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal AuthenticatedUser principal) {
        return taskService.update(id, request, principal.getId());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of your tasks")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        taskService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
