package com.gestionstock.iam.presentation.dto;

public record AuthResponse(
        String token,
        Long userId,
        Long organisationId,
        String role
) {}
