package com.gestionstock.owner.application.dto;

import java.time.LocalDateTime;

public record OwnerSupportUserResponse(
        Long id,
        String email,
        String role,
        boolean enabled,
        LocalDateTime lastLoginAt
) {
}
