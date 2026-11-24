package com.example.albaondo_public.global.security;

import com.example.albaondo_public.user.domain.UserRole;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

/** Immutable account snapshot established after JWT and database validation. */
public record AuthenticatedUser(UUID userId, String username, UserRole role) implements Principal {
    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
        if (username == null || username.isBlank()) throw new IllegalArgumentException(
            "username is required"
        );
    }

    @Override
    public String getName() {
        return username;
    }
}
