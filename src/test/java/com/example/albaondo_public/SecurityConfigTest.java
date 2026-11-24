package com.example.albaondo_public;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.albaondo_public.global.config.SecurityConfig;
import com.example.albaondo_public.global.filter.ApiAccessDeniedHandler;
import com.example.albaondo_public.global.filter.ApiAuthenticationEntryPoint;
import com.example.albaondo_public.global.filter.JwtAuthenticationFilter;
import com.example.albaondo_public.global.security.JwtProvider;
import com.example.albaondo_public.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

class SecurityConfigTest {

    @Test
    void createsProviderWithDefaultHeaderAndRegistersJwtFilter() {
        String testSecret = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
        new WebApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class)
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(ApiAuthenticationEntryPoint.class, () -> mock(ApiAuthenticationEntryPoint.class))
            .withBean(ApiAccessDeniedHandler.class, () -> mock(ApiAccessDeniedHandler.class))
            .withPropertyValues("jwt.secret-key=" + testSecret)
            .run(context -> {
                assertThat(context).hasSingleBean(JwtProvider.class);
                assertThat(context.getBean(JwtProvider.class).getAccessHeader()).isEqualTo("Authorization");
                assertThat(context.getBean(SecurityFilterChain.class).getFilters())
                    .anyMatch(JwtAuthenticationFilter.class::isInstance);
            });
    }
}
