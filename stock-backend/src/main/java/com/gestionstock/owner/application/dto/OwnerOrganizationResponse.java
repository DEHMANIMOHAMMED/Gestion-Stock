package com.gestionstock.owner.application.dto;

import java.time.LocalDateTime;

public record OwnerOrganizationResponse(
        Long id,
        String name,
        String industry,
        String sizeRange,
        String city,
        String country,
        String status,
        String planCode,
        String subscriptionStatus,
        boolean onboardingCompleted,
        LocalDateTime createdAt,
        long usersCount,
        long productsCount,
        long stockMovementsCount
) {
}
