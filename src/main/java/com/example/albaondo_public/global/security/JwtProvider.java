package com.example.albaondo_public.global.security;

import com.example.albaondo_public.global.filter.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/** 접근 토큰을 읽는 부분만 발췌했다. 키와 헤더 설정은 외부에서 받는다. */
@RequiredArgsConstructor
public class JwtProvider {

    private static final String BEARER = "Bearer ";
    private final SecretKey secretKey;
    @Getter
    private final String accessHeader;

    public Optional<String> extractBearerToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(accessHeader))
            .filter(token -> token.startsWith(BEARER))
            .map(token -> token.substring(BEARER.length()))
            .filter(token -> !token.isBlank());
    }

    public Authentication getAuthentication(String token) {
        Claims claims = extractToken(token);
        requireTokenKind(claims, "access");
        CustomUserDetails userDetails = CustomUserDetails.of(claims);
        return new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
    }

    public Claims extractToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    private void requireTokenKind(Claims claims, String kind) {
        if (
            !kind.equals(claims.get("token_kind", String.class)) ||
            claims.getSubject() == null ||
            claims.getSubject().isBlank() ||
            claims.getExpiration() == null
        ) {
            throw new JwtException("Invalid token kind or required claims");
        }
    }
}
