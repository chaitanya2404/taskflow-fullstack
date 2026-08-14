package com.taskflow.backend.repository;

import com.taskflow.backend.entity.Task;
import com.taskflow.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
}
