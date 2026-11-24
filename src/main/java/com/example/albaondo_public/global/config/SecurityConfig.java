package com.example.albaondo_public.global.config;

import com.example.albaondo_public.global.filter.ApiAccessDeniedHandler;
import com.example.albaondo_public.global.filter.ApiAuthenticationEntryPoint;
import com.example.albaondo_public.global.filter.JwtAuthenticationFilter;
import com.example.albaondo_public.global.security.JwtProvider;
import com.example.albaondo_public.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private final JwtProvider jwtProvider;
    private final UserRepository users;
    private final ApiAuthenticationEntryPoint entryPoint;
    private final ApiAccessDeniedHandler deniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize ->
                authorize.anyRequest().authenticated()
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
