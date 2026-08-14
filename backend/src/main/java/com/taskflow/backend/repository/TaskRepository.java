package com.taskflow.backend.repository;

import com.taskflow.backend.entity.Task;
import com.taskflow.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Tasks inherit ownership from their project, so every finder traverses
 * {@code project.owner.id}. See {@link ProjectRepository} for the rationale.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndProjectOwnerId(Long id, Long ownerId);

    List<Task> findByProjectOwnerIdOrderByIdAsc(Long ownerId);

    List<Task> findByProjectIdAndProjectOwnerIdOrderByIdAsc(Long projectId, Long ownerId);

    List<Task> findByProjectOwnerIdAndStatusOrderByIdAsc(Long ownerId, TaskStatus status);

    List<Task> findByProjectIdAndProjectOwnerIdAndStatusOrderByIdAsc(Long projectId, Long ownerId, TaskStatus status);
}
