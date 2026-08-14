package com.taskflow.backend.controller;

import com.taskflow.backend.dto.LoginRequest;
import com.taskflow.backend.dto.RegisterRequest;
import com.taskflow.backend.entity.Role;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared plumbing for the API integration tests: a real Spring context, a real
 * H2 database with the Flyway schema applied, and helpers for obtaining JWTs.
 *
 * <p>Usernames are randomised because the dev H2 database is declared with
 * {@code DB_CLOSE_DELAY=-1} and therefore survives for the whole test JVM,
 * shared across test classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractApiIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected static String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Registers a fresh USER account through the public API and returns its JWT. */
    protected String registerAndGetToken(String username) throws Exception {
        RegisterRequest request = new RegisterRequest(username, username + "@example.com", "s3cret-password");

        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("token").asString();
    }

    /**
     * Creates an ADMIN account directly through the repository and logs in.
     * Registration deliberately cannot mint ADMINs, so tests that need one go
     * around the API exactly the way {@code AdminAccountSeeder} does.
     */
    protected String registerAdminAndGetToken(String username) throws Exception {
        String password = "s3cret-password";
        userRepository.save(new User(username, username + "@example.com",
                passwordEncoder.encode(password), Role.ADMIN));

        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("token").asString();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
