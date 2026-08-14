package com.taskflow.backend.controller;

import com.taskflow.backend.dto.ProjectRequest;
import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.entity.TaskPriority;
import com.taskflow.backend.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The multi-tenancy guarantee, proven end to end: two real accounts, real
 * tokens, and every cross-account access attempt rejected.
 *
 * <p>Cross-user reads and writes return 404 rather than 403 on purpose — a 403
 * would confirm that the id exists and belongs to somebody, which is itself a
 * small information leak.
 */
class ProjectOwnershipIntegrationTest extends AbstractApiIntegrationTest {

    private record Fixture(String token, Long projectId, Long taskId) {
    }

    /** Registers a user and gives them one project containing one task. */
    private Fixture createUserWithProjectAndTask(String namePrefix, String projectName) throws Exception {
        String token = registerAndGetToken(uniqueUsername(namePrefix));

        String projectJson = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProjectRequest(projectName, "owned by " + namePrefix))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long projectId = objectMapper.readTree(projectJson).get("id").asLong();

        String taskJson = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequest(
                                projectName + " task", null, TaskStatus.TODO, TaskPriority.MEDIUM, null, projectId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();

        return new Fixture(token, projectId, taskId);
    }

    @Test
    void listProjects_onlyEverReturnsTheCallersOwnProjects() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Only Project");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Only Project");

        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, bearer(alice.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].name", everyItem(is("Alice Only Project"))));

        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, bearer(bob.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].name", everyItem(is("Bob Only Project"))));
    }

    @Test
    void readingAnotherUsersProject_returns404() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Secret Project");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Project");

        // Bob asks for Alice's project by its real, existing id.
        mockMvc.perform(get("/api/projects/{id}", alice.projectId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void modifyingAnotherUsersProject_returns404AndLeavesItUntouched() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Untouchable");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Project");

        mockMvc.perform(put("/api/projects/{id}", alice.projectId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProjectRequest("Hijacked by Bob", "pwned"))))
                .andExpect(status().isNotFound());

        // Alice's project is exactly as she left it.
        mockMvc.perform(get("/api/projects/{id}", alice.projectId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(alice.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Untouchable")));
    }

    @Test
    void readingAnotherUsersTask_returns404() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Tasks");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Tasks");

        mockMvc.perform(get("/api/tasks/{id}", alice.taskId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob.token())))
                .andExpect(status().isNotFound());

        // ...and listing tasks never surfaces hers either.
        mockMvc.perform(get("/api/tasks").header(HttpHeaders.AUTHORIZATION, bearer(bob.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].projectId", everyItem(is(bob.projectId().intValue()))));
    }

    @Test
    void deletingAnotherUsersTask_returns404AndLeavesItInPlace() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Keeps This");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Project");

        mockMvc.perform(delete("/api/tasks/{id}", alice.taskId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob.token())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tasks/{id}", alice.taskId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(alice.token())))
                .andExpect(status().isOk());
    }

    @Test
    void filteringTasksByAnotherUsersProjectId_returns404NotAnEmptyList() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Filtered");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Project");

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob.token()))
                        .param("projectId", alice.projectId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingATaskInAnotherUsersProject_returns404() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Closed Project");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Project");

        mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequest(
                                "Smuggled task", null, TaskStatus.TODO, TaskPriority.LOW, null, alice.projectId()))))
                .andExpect(status().isNotFound());

        // Alice still has exactly her one original task.
        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(alice.token()))
                        .param("projectId", alice.projectId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void movingATaskIntoAnotherUsersProject_returns404() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Target");
        Fixture bob = createUserWithProjectAndTask("bob", "Bob Source");

        // Bob tries to re-parent his own task into Alice's project.
        mockMvc.perform(put("/api/tasks/{id}", bob.taskId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequest(
                                "Relocated", null, TaskStatus.TODO, TaskPriority.LOW, null, alice.projectId()))))
                .andExpect(status().isNotFound());
    }

    // ---- role-based authorization --------------------------------------------

    @Test
    void deletingAProject_asUser_returns403() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Project");

        mockMvc.perform(delete("/api/projects/{id}", alice.projectId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(alice.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // ...and it really is still there.
        mockMvc.perform(get("/api/projects/{id}", alice.projectId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(alice.token())))
                .andExpect(status().isOk());
    }

    @Test
    void deletingOwnProject_asAdmin_succeeds() throws Exception {
        String adminToken = registerAdminAndGetToken(uniqueUsername("admin"));

        String projectJson = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProjectRequest("Admin Project", "disposable"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long projectId = objectMapper.readTree(projectJson).get("id").asLong();

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    /**
     * The role gets you past the authorization check, but not past ownership:
     * an ADMIN still cannot delete a project belonging to another account.
     */
    @Test
    void deletingAnotherUsersProject_evenAsAdmin_returns404() throws Exception {
        Fixture alice = createUserWithProjectAndTask("alice", "Alice Admin-Proof");
        String adminToken = registerAdminAndGetToken(uniqueUsername("admin"));

        mockMvc.perform(delete("/api/projects/{id}", alice.projectId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{id}", alice.projectId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(alice.token())))
                .andExpect(status().isOk());
    }
}
