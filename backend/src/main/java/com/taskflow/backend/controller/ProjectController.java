package com.taskflow.backend.controller;

import com.taskflow.backend.dto.ProjectRequest;
import com.taskflow.backend.dto.ProjectResponse;
import com.taskflow.backend.security.AuthenticatedUser;
import com.taskflow.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "CRUD operations for the authenticated user's projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "List the projects owned by the authenticated user")
    public List<ProjectResponse> getAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        return projectService.findAll(principal.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of your projects by id",
            description = "Returns 404 for ids that do not exist and for projects owned by someone else.")
    public ProjectResponse getById(@PathVariable Long id,
                                   @AuthenticationPrincipal AuthenticatedUser principal) {
        return projectService.findById(id, principal.getId());
    }

    @PostMapping
    @Operation(summary = "Create a project owned by the authenticated user")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser principal) {
        ProjectResponse created = projectService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of your projects")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProjectRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser principal) {
        return projectService.update(id, request, principal.getId());
    }

    /**
     * Deleting a project cascades to its tasks, so it is restricted to ADMIN
     * accounts. This is the project's role-based authorization example: ownership
     * alone is not enough, the caller also needs the role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a project and its tasks (ADMIN only)",
            description = "Requires the ADMIN role; a USER token gets 403. "
                    + "ADMINs may still only delete projects they own.")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        projectService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
