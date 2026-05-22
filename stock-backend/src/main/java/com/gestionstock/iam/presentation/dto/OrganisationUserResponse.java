package com.gestionstock.iam.presentation.dto;

public record OrganisationUserResponse(
        Long id,
        String email,
        Long organisationId,
        String organisationName,
        String role,
        boolean enabled,
        java.time.LocalDateTime lastLoginAt
) {
}
