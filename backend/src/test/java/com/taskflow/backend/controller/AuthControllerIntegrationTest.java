package com.taskflow.backend.controller;

import com.taskflow.backend.dto.LoginRequest;
import com.taskflow.backend.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registration, login, token validation and the public/protected boundary. */
class AuthControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    void register_thenLogin_returnsUsableJwt() throws Exception {
        String username = uniqueUsername("newuser");
        RegisterRequest register = new RegisterRequest(username, username + "@example.com", "s3cret-password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(username)))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                // three dot-separated base64url segments
                .andExpect(jsonPath("$.token", matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")));

        String loginJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "s3cret-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginJson).get("token").asString();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void register_neverLeaksThePassword() throws Exception {
        String username = uniqueUsername("nopass");
        RegisterRequest register = new RegisterRequest(username, username + "@example.com", "s3cret-password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token", not(is("s3cret-password"))));
    }

    @Test
    void register_withInvalidPayload_returns400WithFieldErrors() throws Exception {
        RegisterRequest bad = new RegisterRequest("ab", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void register_withDuplicateUsername_returns409() throws Exception {
        String username = uniqueUsername("dupe");
        RegisterRequest first = new RegisterRequest(username, username + "@example.com", "s3cret-password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = new RegisterRequest(username, username + ".other@example.com", "s3cret-password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("username")));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String username = uniqueUsername("wrongpw");
        registerAndGetToken(username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "definitely-wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    void login_withUnknownUser_returnsTheSame401AsAWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(uniqueUsername("ghost"), "whatever-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withTokenSignedByADifferentKey_returns401() throws Exception {
        // Same header/payload shape, forged signature.
        String forged = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJhZG1pbiIsInVpZCI6MSwicm9sZSI6IkFETUlOIiwiaXNzIjoidGFza2Zsb3cifQ"
                + ".ZmFrZS1zaWduYXR1cmUtdGhhdC1kb2VzLW5vdC12ZXJpZnk";

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoints_remainReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
