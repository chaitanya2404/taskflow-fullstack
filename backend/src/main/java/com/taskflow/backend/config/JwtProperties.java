package com.taskflow.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT signing configuration, bound from {@code taskflow.jwt.*}.
 *
 * <p>The secret is never hardcoded in Java. It comes from configuration, which
 * in turn reads the {@code JWT_SECRET} environment variable; application.yml
 * supplies a clearly-labelled development-only fallback so the project runs with
 * zero setup. Deployments must override it (docker-compose requires it to be set).
 *
 * <p>The {@code @Size(min = 32)} constraint is not cosmetic: HMAC-SHA256 requires
 * a key of at least 256 bits, and JJWT throws at startup for anything shorter.
 * Failing here instead produces a readable configuration error.
 */
@Validated
@ConfigurationProperties(prefix = "taskflow.jwt")
public record JwtProperties(

        @NotBlank(message = "taskflow.jwt.secret must be set (env var JWT_SECRET)")
        @Size(min = 32, message = "taskflow.jwt.secret must be at least 32 characters (256 bits) for HS256")
        String secret,

        @Positive(message = "taskflow.jwt.expiration-minutes must be positive")
        long expirationMinutes,

        @NotBlank(message = "taskflow.jwt.issuer must be set")
        String issuer) {
}
