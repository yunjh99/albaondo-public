package com.example.albaondo_public;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.albaondo_public.global.filter.ApiAuthenticationEntryPoint;
import com.example.albaondo_public.global.filter.JwtAuthenticationFilter;
import com.example.albaondo_public.global.security.AuthenticatedUser;
import com.example.albaondo_public.global.security.JwtProvider;
import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.domain.UserRole;
import com.example.albaondo_public.user.domain.UserStatus;
import com.example.albaondo_public.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class JwtAuthenticationFilterTest {
    private final SecretKey key = Jwts.SIG.HS256.key().build();
    private final UUID id = UUID.randomUUID();
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
        new JwtProvider(key, "Authorization", Duration.ofMinutes(15), Duration.ofDays(14)),
        new ApiAuthenticationEntryPoint(new ObjectMapper()), users
    );
    private final MockFilterChain chain = new MockFilterChain();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedPrincipalContainsVerifiedAccountAndRole() throws Exception {
        activeAccount(1);
        send("Bearer " + token("access", 1));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(id, ((AuthenticatedUser) auth.getPrincipal()).userId());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
        assertNotNull(chain.getRequest());
    }

    @Test
    void revokedVersionIsRejectedBeforeController() throws Exception {
        activeAccount(2);
        send("Bearer " + token("access", 1));
        assertRejected();
    }

    @Test
    void refreshTokenCannotAuthenticateAnApiRequest() throws Exception {
        send("Bearer " + token("refresh", 1));
        assertRejected();
        verifyNoInteractions(users);
    }

    @Test
    void missingHeaderLeavesAuthorizationToSecurityChain() throws Exception {
        send(null);
        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rawTokenWithoutBearerPrefixIsRejected() throws Exception {
        send(token("access", 1));
        assertRejected();
        verifyNoInteractions(users);
    }

    private void activeAccount(long version) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getTokenVersion()).thenReturn(version);
        when(users.findById(id)).thenReturn(Optional.of(user));
    }

    private String token(String kind, long version) {
        return Jwts.builder().subject(id.toString()).claim("role", "USER")
            .claim("token_kind", kind).claim("version", version)
            .expiration(Date.from(Instant.now().plusSeconds(300))).signWith(key).compact();
    }

    private void send(String header) throws Exception {
        var request = new MockHttpServletRequest();
        if (header != null) request.addHeader("Authorization", header);
        filter.doFilter(request, response, chain);
    }

    private void assertRejected() {
        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
