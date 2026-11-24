package com.example.albaondo_public.global.filter;

import com.example.albaondo_public.global.security.AuthenticatedUser;
import com.example.albaondo_public.global.security.JwtProvider;
import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ApiAuthenticationEntryPoint entryPoint;
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader(jwtProvider.getAccessHeader());
        if (header != null) {
            try {
                Authentication authentication = authenticate(header);
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (BadCredentialsException e) {
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, e);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Authentication authenticate(String header) {
        JwtProvider.TokenClaims claims = jwtProvider.verifyAccessToken(header);
        User user = users.findById(claims.userId())
            .orElseThrow(() -> new BadCredentialsException("Account is unavailable"));

        claims.verifyAccount(user);

        var principal = new AuthenticatedUser(user.getId(), user.getId().toString(), user.getRole());
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(authority));
    }
}
