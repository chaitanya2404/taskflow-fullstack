package com.taskflow.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.dto.ProjectRequest;
import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.entity.TaskPriority;
import com.taskflow.backend.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full end-to-end integration test: real Spring context, real (H2, in-memory)
 * database, requests dispatched through MockMvc into the actual controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullTaskLifecycle_createReadUpdateFilterDelete() throws Exception {
        // 1. Create a project to hang the task off of
        ProjectRequest projectRequest = new ProjectRequest("Integration Test Project", "Created by MockMvc test");
        String projectJson = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Integration Test Project")))
                .andReturn().getResponse().getContentAsString();

        Long projectId = objectMapper.readTree(projectJson).get("id").asLong();

        // 2. Create a task under that project
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Set up CI pipeline");
        taskRequest.setDescription("Configure GitHub Actions for build + test");
        taskRequest.setStatus(TaskStatus.TODO);
        taskRequest.setPriority(TaskPriority.HIGH);
        taskRequest.setDueDate(LocalDate.of(2026, 9, 30));
        taskRequest.setProjectId(projectId);

        String taskJson = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Set up CI pipeline")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.projectId", is(projectId.intValue())))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();

        // 3. Read it back directly
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Set up CI pipeline")));

        // 4. Filter tasks by project id
        mockMvc.perform(get("/api/tasks").param("projectId", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId", is(projectId.intValue())));

        // 5. Filter tasks by status
        mockMvc.perform(get("/api/tasks").param("status", "TODO"))
                .andExpect(status().isOk());

        // 6. Update the task
        taskRequest.setStatus(TaskStatus.IN_PROGRESS);
        taskRequest.setTitle("Set up CI pipeline (in progress)");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // 7. Validation failure: blank title should return 400 with field errors
        TaskRequest invalidRequest = new TaskRequest();
        invalidRequest.setTitle(""); // blank -> @NotBlank violation
        invalidRequest.setStatus(TaskStatus.TODO);
        invalidRequest.setPriority(TaskPriority.LOW);
        invalidRequest.setProjectId(projectId);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("title")));

        // 8. Not found: fetching a bogus id returns 404
        mockMvc.perform(get("/api/tasks/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        // 9. Delete the task
        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound());
    }
}
