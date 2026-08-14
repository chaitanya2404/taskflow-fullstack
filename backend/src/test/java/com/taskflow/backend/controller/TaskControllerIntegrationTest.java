package com.taskflow.backend.controller;

import com.taskflow.backend.dto.ProjectRequest;
import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.entity.TaskPriority;
import com.taskflow.backend.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full end-to-end integration test: real Spring context, real (H2, in-memory)
 * database with the Flyway schema applied, requests dispatched through MockMvc
 * into the actual controller — now carrying a real JWT obtained from
 * {@code /api/auth/register}.
 */
class TaskControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    void fullTaskLifecycle_createReadUpdateFilterDelete() throws Exception {
        String token = registerAndGetToken(uniqueUsername("lifecycle"));

        // 1. Create a project to hang the task off of
        ProjectRequest projectRequest = new ProjectRequest("Integration Test Project", "Created by MockMvc test");
        String projectJson = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Integration Test Project")))
                .andReturn().getResponse().getContentAsString();

        Long projectId = objectMapper.readTree(projectJson).get("id").asLong();

        // 2. Create a task under that project
        TaskRequest taskRequest = new TaskRequest(
                "Set up CI pipeline",
                "Configure GitHub Actions for build + test",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 9, 30),
                projectId);

        String taskJson = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Set up CI pipeline")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.projectId", is(projectId.intValue())))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();

        // 3. Read it back directly
        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Set up CI pipeline")));

        // 4. Filter tasks by project id
        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("projectId", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId", is(projectId.intValue())));

        // 5. Filter tasks by status
        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("status", "TODO"))
                .andExpect(status().isOk());

        // 6. Update the task
        TaskRequest updateRequest = new TaskRequest(
                "Set up CI pipeline (in progress)",
                taskRequest.description(),
                TaskStatus.IN_PROGRESS,
                taskRequest.priority(),
                taskRequest.dueDate(),
                taskRequest.projectId());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // 7. Validation failure: blank title should return 400 with field errors
        TaskRequest invalidRequest = new TaskRequest(
                "", // blank -> @NotBlank violation
                null,
                TaskStatus.TODO,
                TaskPriority.LOW,
                null,
                projectId);

        mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("title")));

        // 8. Not found: fetching a bogus id returns 404
        mockMvc.perform(get("/api/tasks/{id}", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        // 9. Delete the task
        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }
}
