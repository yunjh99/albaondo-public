package com.example.albaondo_public.global.security;

import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TokenRefreshController {
    private final JwtProvider tokens;
    private final UserRepository users;

    public record AccessTokenResponse(String accessToken) {}

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
        @RequestHeader(value = "${jwt.refresh.header:X-Refresh-Token}", required = false) String header
    ) {
        JwtProvider.TokenClaims claims = tokens.verifyRefreshToken(header);
        User user = users.findById(claims.userId())
            .orElseThrow(() -> new BadCredentialsException("Account is unavailable"));
        claims.verifyAccount(user);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new AccessTokenResponse(tokens.issueAccessToken(claims)));
    }
}
