package com.taskflow.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the bearer-token scheme so the Swagger UI shows an "Authorize" button.
 *
 * <p>Paste the {@code token} value from {@code POST /api/auth/login} into it and
 * every subsequent "Try it out" call carries the {@code Authorization} header.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI taskflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskFlow API")
                        .version("v1")
                        .description("""
                                Project and task tracker.

                                All /api/projects and /api/tasks endpoints require a JWT.
                                Obtain one from POST /api/auth/register or POST /api/auth/login,
                                then click Authorize and paste the token.

                                Data is scoped per account: you only ever see your own projects
                                and their tasks.""")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT issued by /api/auth/login")))
                // Applied globally; the auth endpoints are still reachable without it.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
