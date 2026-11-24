package com.example.albaondo_public.global.security;

import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.domain.UserRole;
import com.example.albaondo_public.user.domain.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;

@RequiredArgsConstructor
public class JwtProvider {

    private static final String BEARER = "Bearer ";
    private final SecretKey secretKey;
    @Getter
    private final String accessHeader;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;

    public record TokenPair(String accessToken, String refreshToken) {}

    public record TokenClaims(UUID userId, UserRole role, long version) {
        public void verifyAccount(User user) {
            if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null
                || role != user.getRole() || version != user.getTokenVersion()) {
                throw new BadCredentialsException("Token no longer matches the account");
            }
        }
    }

    /** Called by the login flow after authenticating the account. */
    public TokenPair issueTokens(User user) {
        var claims = new TokenClaims(user.getId(), user.getRole(), user.getTokenVersion());
        claims.verifyAccount(user);
        return new TokenPair(issue("access", claims, accessExpiration), issue("refresh", claims, refreshExpiration));
    }

    TokenClaims verifyRefreshToken(String header) {
        return verifyToken(header, "refresh");
    }

    String issueAccessToken(TokenClaims claims) {
        return issue("access", claims, accessExpiration);
    }

    private String issue(String kind, TokenClaims claims, Duration lifetime) {
        Instant now = Instant.now();
        return Jwts.builder().subject(claims.userId().toString())
            .id(UUID.randomUUID().toString()).claim("token_kind", kind)
            .claim("role", claims.role().name()).claim("version", claims.version())
            .issuedAt(Date.from(now)).expiration(Date.from(now.plus(lifetime)))
            .signWith(secretKey).compact();
    }

    public TokenClaims verifyAccessToken(String header) {
        return verifyToken(header, "access");
    }

    private TokenClaims verifyToken(String header, String kind) {
        String token = extractBearerToken(header);
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .require("token_kind", kind)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return readClaims(claims);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid " + kind + " token", e);
        }
    }

    private String extractBearerToken(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            throw new BadCredentialsException("Bearer token is required");
        }
        String token = header.substring(BEARER.length());
        if (token.isBlank()) {
            throw new BadCredentialsException("Bearer token is required");
        }
        return token;
    }

    private TokenClaims readClaims(Claims claims) {
        String subject = claims.getSubject();
        String role = claims.get("role", String.class);
        Long version = claims.get("version", Long.class);
        if (subject == null || role == null || version == null || claims.getExpiration() == null) {
            throw new BadCredentialsException("Required token claim is missing");
        }
        return new TokenClaims(UUID.fromString(subject), UserRole.valueOf(role), version);
    }
}
