package com.example.albaondo_public.global.config;

import com.example.albaondo_public.global.filter.ApiAccessDeniedHandler;
import com.example.albaondo_public.global.filter.ApiAuthenticationEntryPoint;
import com.example.albaondo_public.global.filter.JwtAuthenticationFilter;
import com.example.albaondo_public.global.security.JwtProvider;
import com.example.albaondo_public.user.repository.UserRepository;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository users;
    private final ApiAuthenticationEntryPoint entryPoint;
    private final ApiAccessDeniedHandler deniedHandler;

    @Bean
    public JwtProvider jwtProvider(
        @Value("${jwt.secret-key}") String secretKey,
        @Value("${jwt.access-header:Authorization}") String accessHeader,
        @Value("${jwt.access.expiration:PT15M}") String accessExpiration,
        @Value("${jwt.refresh.expiration:P14D}") String refreshExpiration
    ) {
        return new JwtProvider(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), accessHeader,
            parseTokenLifetime(accessExpiration), parseTokenLifetime(refreshExpiration));
    }

    private Duration parseTokenLifetime(String value) {
        Duration lifetime = Duration.parse(value);
        if (lifetime.getSeconds() < 1) {
            throw new IllegalArgumentException("Token lifetimes must be at least one second");
        }
        return lifetime;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {
        http.formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize ->
                authorize.requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler)
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtProvider, entryPoint, users),
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
    }

}
