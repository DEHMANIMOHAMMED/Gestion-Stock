package com.gestionstock.iam.presentation.dto;

public record OrganisationProfileResponse(
        Long organisationId,
        String name,
        String industry,
        String sizeRange,
        String phone,
        String address,
        String city,
        String country,
        String currency,
        String logoUrl,
        String taxId,
        String website,
        String stockAlertEmail,
        Integer defaultLeadTimeDays,
        boolean onboardingCompleted
) {
}
