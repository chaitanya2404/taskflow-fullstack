package com.taskflow.backend.service;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.entity.Project;
import com.taskflow.backend.entity.Task;
import com.taskflow.backend.entity.TaskPriority;
import com.taskflow.backend.entity.TaskStatus;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    private Project project;
    private Task task;

    @BeforeEach
    void setUp() throws Exception {
        project = new Project("Website Revamp", "Redesign the marketing site");
        setId(project, 1L);

        task = new Task("Design homepage", "Create wireframes", TaskStatus.TODO, TaskPriority.HIGH,
                LocalDate.of(2026, 9, 1), project);
        setTaskId(task, 10L);
    }

    private void setId(Project p, Long id) throws Exception {
        Field field = Project.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(p, id);
    }

    private void setTaskId(Task t, Long id) throws Exception {
        Field field = Task.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(t, id);
    }

    @Test
    void findById_whenFound_returnsResponse() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.findById(10L);

        assertThat(response.title()).isEqualTo("Design homepage");
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByProject_whenProjectMissing_throwsNotFound() {
        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findByProject(5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).findByProjectId(any());
    }

    @Test
    void findByProject_whenProjectExists_returnsTasks() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.findByProject(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).projectName()).isEqualTo("Website Revamp");
    }

    @Test
    void findByStatus_returnsFilteredTasks() {
        when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.findByStatus(TaskStatus.TODO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void create_whenProjectExists_savesTask() {
        TaskRequest request = new TaskRequest(
                "Write tests",
                "Add unit tests for TaskService",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDate.of(2026, 9, 15),
                1L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            setTaskId(t, 55L);
            return t;
        });

        TaskResponse response = taskService.create(request);

        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.title()).isEqualTo("Write tests");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void create_whenProjectMissing_throwsNotFoundAndDoesNotSave() {
        TaskRequest request = new TaskRequest(
                "Orphan task",
                null,
                TaskStatus.TODO,
                TaskPriority.LOW,
                null,
                404L);

        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void update_whenFound_updatesFields() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        TaskRequest request = new TaskRequest(
                "Design homepage v2",
                "Updated wireframes",
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                LocalDate.of(2026, 10, 1),
                1L);

        TaskResponse response = taskService.update(10L, request);

        assertThat(response.title()).isEqualTo("Design homepage v2");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.URGENT);
    }

    @Test
    void delete_whenFound_deletesEntity() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        taskService.delete(10L);

        verify(taskRepository, times(1)).delete(task);
    }
}
