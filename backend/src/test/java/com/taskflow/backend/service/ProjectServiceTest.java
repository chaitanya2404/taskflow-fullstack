package com.taskflow.backend.service;

import com.taskflow.backend.dto.ProjectRequest;
import com.taskflow.backend.dto.ProjectResponse;
import com.taskflow.backend.entity.Project;
import com.taskflow.backend.entity.Role;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
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
class ProjectServiceTest {

    private static final Long OWNER_ID = 7L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        owner = new User("alice", "alice@example.com", "{bcrypt}hash", Role.USER);
        owner.setId(OWNER_ID);

        project = new Project("Website Revamp", "Redesign the marketing site", owner);
        setId(project, 1L);
    }

    private void setId(Project p, Long id) throws Exception {
        Field field = Project.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(p, id);
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(projectRepository.findByOwnerIdOrderByIdAsc(OWNER_ID)).thenReturn(List.of(project));

        List<ProjectResponse> result = projectService.findAll(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Website Revamp");
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void findById_whenFound_returnsResponse() {
        when(projectRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.findById(1L, OWNER_ID);

        assertThat(response.name()).isEqualTo("Website Revamp");
        assertThat(response.taskCount()).isZero();
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(projectRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(99L, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesAndReturnsResponse() {
        ProjectRequest request = new ProjectRequest("New Project", "A brand new project");
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            setId(p, 42L);
            return p;
        });

        ProjectResponse response = projectService.create(request, OWNER_ID);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("New Project");
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void update_whenFound_updatesFields() {
        when(projectRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(project));
        ProjectRequest request = new ProjectRequest("Renamed Project", "Updated description");

        ProjectResponse response = projectService.update(1L, request, OWNER_ID);

        assertThat(response.name()).isEqualTo("Renamed Project");
        assertThat(response.description()).isEqualTo("Updated description");
    }

    @Test
    void update_whenMissing_throwsNotFound() {
        when(projectRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());
        ProjectRequest request = new ProjectRequest("X", "Y");

        assertThatThrownBy(() -> projectService.update(99L, request, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenFound_deletesEntity() {
        when(projectRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(project));

        projectService.delete(1L, OWNER_ID);

        verify(projectRepository, times(1)).delete(project);
    }

    @Test
    void delete_whenMissing_throwsNotFoundAndDoesNotDelete() {
        when(projectRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(99L, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectRepository, never()).delete(any());
    }

    /**
     * The ownership guarantee at the unit level: the service never calls a
     * repository method that could return a row belonging to a different user.
     * A project that exists but is owned by someone else is simply not found.
     */
    @Test
    void findById_whenProjectBelongsToAnotherUser_throwsNotFound() {
        Long otherUserId = 999L;
        when(projectRepository.findByIdAndOwnerId(1L, otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(1L, otherUserId))
                .isInstanceOf(ResourceNotFoundException.class);

        // and crucially: no owner-agnostic lookup was ever attempted
        verify(projectRepository, never()).findById(any());
    }
}
