package com.taskflow.backend.service;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.entity.Project;
import com.taskflow.backend.entity.Task;
import com.taskflow.backend.entity.TaskStatus;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Task business logic.
 *
 * <p>Tasks are owned transitively through their project, so every query is
 * filtered on {@code project.owner.id}. As in {@link ProjectService}, another
 * user's task reads as "not found".
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(Long ownerId) {
        return taskRepository.findByProjectOwnerIdOrderByIdAsc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id, Long ownerId) {
        return toResponse(getOwnedTaskOrThrow(id, ownerId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findByProject(Long projectId, Long ownerId) {
        // Check the project first so an unknown (or someone else's) project id
        // gives a 404 rather than an empty list.
        getOwnedProjectOrThrow(projectId, ownerId);
        return taskRepository.findByProjectIdAndProjectOwnerIdOrderByIdAsc(projectId, ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findByStatus(TaskStatus status, Long ownerId) {
        return taskRepository.findByProjectOwnerIdAndStatusOrderByIdAsc(ownerId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findByProjectAndStatus(Long projectId, TaskStatus status, Long ownerId) {
        getOwnedProjectOrThrow(projectId, ownerId);
        return taskRepository.findByProjectIdAndProjectOwnerIdAndStatusOrderByIdAsc(projectId, ownerId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse create(TaskRequest request, Long ownerId) {
        Project project = getOwnedProjectOrThrow(request.projectId(), ownerId);
        Task task = new Task(
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate(),
                project
        );
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request, Long ownerId) {
        Task task = getOwnedTaskOrThrow(id, ownerId);

        if (!task.getProject().getId().equals(request.projectId())) {
            // Re-parenting is only allowed into a project the caller also owns.
            task.setProject(getOwnedProjectOrThrow(request.projectId(), ownerId));
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return toResponse(task);
    }

    public void delete(Long id, Long ownerId) {
        taskRepository.delete(getOwnedTaskOrThrow(id, ownerId));
    }

    private Task getOwnedTaskOrThrow(Long id, Long ownerId) {
        return taskRepository.findByIdAndProjectOwnerId(id, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Task", id));
    }

    private Project getOwnedProjectOrThrow(Long id, Long ownerId) {
        return projectRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Project", id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getProject().getId(),
                task.getProject().getName()
        );
    }
}
