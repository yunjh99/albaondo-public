package com.example.albaondo_public.global.filter;

import com.example.albaondo_public.global.security.AuthenticatedUser;
import com.example.albaondo_public.global.security.JwtProvider;
import com.example.albaondo_public.user.domain.UserStatus;
import com.example.albaondo_public.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ApiAuthenticationEntryPoint entryPoint;
    private final UserRepository users;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(jwtProvider.getAccessHeader());
        if (header == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = jwtProvider
                .extractBearerToken(request)
                .orElseThrow(() -> new BadCredentialsException("Bearer token is required"));
            var authentication = jwtProvider.getAuthentication(token);
            var principal = (CustomUserDetails) authentication.getPrincipal();
            var user = users
                .findById(UUID.fromString(principal.getUsername()))
                .orElseThrow(() -> new BadCredentialsException("Account is unavailable"));
            Long version = jwtProvider.extractToken(token).get("version", Long.class);
            if (
                version == null || version != user.getTokenVersion()
            ) throw new BadCredentialsException("Token revoked");
            if (
                user.getStatus() != UserStatus.ACTIVE ||
                user.getDeletedAt() != null ||
                user.getRole() != principal.role()
            ) {
                throw new BadCredentialsException("Account is inactive or role has changed");
            }
            var authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getId().toString(),
                user.getRole()
            );
            var verifiedAuthentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                principal.getAuthorities()
            );
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(verifiedAuthentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | BadCredentialsException e) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(
                request,
                response,
                new BadCredentialsException("Invalid access token", e)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
