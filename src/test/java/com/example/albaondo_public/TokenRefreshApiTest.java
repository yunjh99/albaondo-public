package com.example.albaondo_public;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.albaondo_public.global.config.SecurityConfig;
import com.example.albaondo_public.global.filter.ApiAccessDeniedHandler;
import com.example.albaondo_public.global.filter.ApiAuthenticationEntryPoint;
import com.example.albaondo_public.global.security.JwtProvider;
import com.example.albaondo_public.global.security.TokenRefreshController;
import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.domain.UserRole;
import com.example.albaondo_public.user.domain.UserStatus;
import com.example.albaondo_public.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

@SpringJUnitWebConfig(TokenRefreshApiTest.Config.class)
class TokenRefreshApiTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, TokenRefreshController.class, ApiAccessDeniedHandler.class,
        ApiAuthenticationEntryPoint.class, ObjectMapper.class})
    static class Config {}

    private static final String SECRET = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
    private static final String PATH = "/api/auth/refresh";
    private static final String HEADER = "X-Test-Refresh";

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry properties) {
        properties.add("jwt.secret-key", () -> SECRET);
        properties.add("jwt.access.expiration", () -> "PT3M");
        properties.add("jwt.refresh.expiration", () -> "P2D");
        properties.add("jwt.refresh.header", () -> HEADER);
    }

    @Autowired WebApplicationContext context;
    @Autowired JwtProvider tokens;
    @MockitoBean UserRepository users;
    private final User user = mock(User.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        UUID id = UUID.randomUUID();
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(users.findById(id)).thenReturn(Optional.of(user));
    }

    @Test
    void issuesBothLifetimesAndRenewsOnlyAccess() throws Exception {
        var pair = tokens.issueTokens(user);
        assertLifetime(pair.accessToken(), Duration.ofMinutes(3));
        assertLifetime(pair.refreshToken(), Duration.ofDays(2));
        String body = mvc.perform(post(PATH).header(HEADER, "Bearer " + pair.refreshToken()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn().getResponse().getContentAsString();
        String access = context.getBean(ObjectMapper.class).readTree(body).get("accessToken").asString();
        assertThat(access).isNotEqualTo(pair.accessToken());
        assertThat(tokens.verifyAccessToken("Bearer " + access).userId()).isEqualTo(user.getId());
        assertLifetime(access, Duration.ofMinutes(3));
    }

    private void assertLifetime(String token, Duration expected) {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
            .isEqualTo(expected);
    }

    @Test
    void rejectsMissingWrongKindExpiredAndRevokedTokens() throws Exception {
        var pair = tokens.issueTokens(user);
        mvc.perform(post(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).header(HEADER, "Bearer " + pair.accessToken())).andExpect(status().isUnauthorized());
        for (String kind : new String[]{"refresh", "access"}) {
            String expired = Jwts.builder().subject(user.getId().toString()).claim("role", "USER")
                .claim("version", 0).claim("token_kind", kind).expiration(Date.from(Instant.now().minusSeconds(5)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
            boolean refresh = kind.equals("refresh");
            mvc.perform(post(refresh ? PATH : "/api/reviews")
                .header(refresh ? HEADER : "Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
        }
        when(user.getTokenVersion()).thenReturn(1L);
        mvc.perform(post(PATH).header(HEADER, "Bearer " + pair.refreshToken())).andExpect(status().isUnauthorized());
    }
}
