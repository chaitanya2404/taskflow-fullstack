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

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(getTaskOrThrow(id));
    }

    public List<TaskResponse> findByProject(Long projectId) {
        // ensure the project exists before filtering, so callers get a 404 instead of an empty list
        getProjectOrThrow(projectId);
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TaskResponse> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TaskResponse> findByProjectAndStatus(Long projectId, TaskStatus status) {
        getProjectOrThrow(projectId);
        return taskRepository.findByProjectIdAndStatus(projectId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse create(TaskRequest request) {
        Project project = getProjectOrThrow(request.projectId());
        Task task = new Task(
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate(),
                project
        );
        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getTaskOrThrow(id);

        if (!task.getProject().getId().equals(request.projectId())) {
            Project newProject = getProjectOrThrow(request.projectId());
            task.setProject(newProject);
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return toResponse(task);
    }

    public void delete(Long id) {
        Task task = getTaskOrThrow(id);
        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Task", id));
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
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
