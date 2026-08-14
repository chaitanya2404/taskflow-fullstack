package com.taskflow.backend.entity;

/**
 * Application roles.
 *
 * <p>Stored in the database as the bare name ({@code USER} / {@code ADMIN}).
 * Spring Security's {@code hasRole(...)} expression prepends the {@code ROLE_}
 * prefix, so authorities are built as {@code ROLE_ + name()} — see
 * {@link com.taskflow.backend.security.AuthenticatedUser}.
 */
public enum Role {
    USER,
    ADMIN
}
