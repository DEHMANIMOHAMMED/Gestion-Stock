package com.gestionstock.owner.application.dto;

import java.util.List;

public record OwnerDashboardResponse(
        long organizationsCount,
        long usersCount,
        long productsCount,
        long stockMovementsCount,
        long activeOrganizationsCount,
        long trialOrganizationsCount,
        List<OwnerOrganizationResponse> organizations,
        LegalSettingsResponse legalSettings
) {
}
