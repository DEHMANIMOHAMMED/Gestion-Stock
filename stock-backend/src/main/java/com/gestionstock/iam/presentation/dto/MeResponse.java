package com.gestionstock.iam.presentation.dto;

public record MeResponse(
        Long userId,
        String email,
        Long organisationId,
        String organisationName,
        boolean onboardingCompleted,
        String role
) {}
