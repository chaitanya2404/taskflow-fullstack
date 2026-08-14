package com.taskflow.backend.security;

import com.taskflow.backend.config.JwtProperties;
import com.taskflow.backend.entity.Role;
import com.taskflow.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Direct tests of the signing/verification primitive. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hs256";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 60, "taskflow"));
        user = new User("alice", "alice@example.com", "{bcrypt}hash", Role.ADMIN);
        user.setId(42L);
    }

    @Test
    void generatedToken_roundTripsSubjectAndClaims() {
        Claims claims = jwtService.parseClaims(jwtService.generateToken(user));

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("uid", Number.class).longValue()).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getIssuer()).isEqualTo("taskflow");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void tokenSignedWithADifferentSecret_isRejected() {
        JwtService attacker = new JwtService(
                new JwtProperties("a-completely-different-secret-key-of-sufficient-length", 60, "taskflow"));

        String forged = attacker.generateToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromADifferentIssuer_isRejected() {
        JwtService otherIssuer = new JwtService(new JwtProperties(SECRET, 60, "some-other-app"));

        assertThatThrownBy(() -> jwtService.parseClaims(otherIssuer.generateToken(user)))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_isRejected() {
        // Correctly signed by us, but issued an hour ago with a one-minute life.
        Instant anHourAgo = Instant.now().minus(Duration.ofHours(1));
        String expired = Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuer("taskflow")
                .issuedAt(Date.from(anHourAgo))
                .expiration(Date.from(anHourAgo.plus(Duration.ofMinutes(1))))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenWithATamperedSignature_isRejected() {
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".bm90LWEtc2lnbmF0dXJl";

        assertThatThrownBy(() -> jwtService.parseClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiresInSeconds_reflectsConfiguredLifetime() {
        assertThat(jwtService.expiresInSeconds()).isEqualTo(3600L);
    }
}
