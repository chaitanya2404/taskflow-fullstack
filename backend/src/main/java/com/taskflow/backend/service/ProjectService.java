package com.taskflow.backend.service;

import com.taskflow.backend.dto.ProjectRequest;
import com.taskflow.backend.dto.ProjectResponse;
import com.taskflow.backend.entity.Project;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Project business logic.
 *
 * <p>Every public method takes the authenticated {@code ownerId} and pushes it
 * into the query. A project belonging to someone else is indistinguishable from
 * one that does not exist — both produce a 404 rather than a 403, so the API
 * does not leak which project ids are in use.
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(Long ownerId) {
        return projectRepository.findByOwnerIdOrderByIdAsc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id, Long ownerId) {
        return toResponse(getOwnedProjectOrThrow(id, ownerId));
    }

    public ProjectResponse create(ProjectRequest request, Long ownerId) {
        // getReferenceById avoids a SELECT: the owner is already authenticated,
        // so we only need the FK value, not the row.
        Project project = new Project(request.name(), request.description(),
                userRepository.getReferenceById(ownerId));
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(Long id, ProjectRequest request, Long ownerId) {
        Project project = getOwnedProjectOrThrow(id, ownerId);
        project.setName(request.name());
        project.setDescription(request.description());
        return toResponse(project);
    }

    public void delete(Long id, Long ownerId) {
        projectRepository.delete(getOwnedProjectOrThrow(id, ownerId));
    }

    private Project getOwnedProjectOrThrow(Long id, Long ownerId) {
        return projectRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Project", id));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getTasks().size()
        );
    }
}
