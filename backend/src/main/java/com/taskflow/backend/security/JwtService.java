package com.taskflow.backend.security;

import com.taskflow.backend.config.JwtProperties;
import com.taskflow.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies HMAC-SHA256 JSON Web Tokens.
 *
 * <p>Tokens are stateless: the subject is the username and a {@code uid} claim
 * carries the database id. Nothing is stored server-side, so "logout" is a
 * client-side token discard — see the README for the trade-off.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;
    private final String issuer;
    private final JwtParser parser;

    public JwtService(JwtProperties properties) {
        // hmacShaKeyFor selects HS256/384/512 from the key length; the >=32 char
        // constraint on JwtProperties guarantees at least HS256.
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
        this.issuer = properties.issuer();
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build();
    }

    /** Signs a token for the given user. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies the signature, issuer and expiry, returning the claims.
     *
     * @throws JwtException if the token is malformed, expired, or not signed by us
     */
    public Claims parseClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    /** Seconds until an issued token expires — surfaced in the login response. */
    public long expiresInSeconds() {
        return expiration.toSeconds();
    }
}
