package com.taskflow.backend.service;

import com.taskflow.backend.dto.ProjectRequest;
import com.taskflow.backend.dto.ProjectResponse;
import com.taskflow.backend.entity.Project;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<ProjectResponse> findAll() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        Project project = getProjectOrThrow(id);
        return toResponse(project);
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project(request.name(), request.description());
        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getProjectOrThrow(id);
        project.setName(request.name());
        project.setDescription(request.description());
        return toResponse(project);
    }

    public void delete(Long id) {
        Project project = getProjectOrThrow(id);
        projectRepository.delete(project);
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
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
