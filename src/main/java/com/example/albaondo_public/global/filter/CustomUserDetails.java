package com.example.albaondo_public.global.filter;

import com.example.albaondo_public.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CustomUserDetails(String username, UserRole role) implements UserDetails {
    public static CustomUserDetails of(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null) {
            throw new JwtException("UserRole claim is required");
        }
        return new CustomUserDetails(claims.getSubject(), UserRole.valueOf(role));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
