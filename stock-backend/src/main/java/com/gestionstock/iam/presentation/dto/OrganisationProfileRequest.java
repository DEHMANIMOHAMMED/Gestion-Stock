package com.gestionstock.iam.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record OrganisationProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String industry,
        @NotBlank @Size(max = 40) String sizeRange,
        @Size(max = 60) String phone,
        @Size(max = 220) String address,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String country,
        @NotBlank @Size(max = 10) String currency,
        @Size(max = 500) String logoUrl,
        @Size(max = 80) String taxId,
        @Size(max = 220) String website,
        @Size(max = 180) String stockAlertEmail,
        @Min(0) Integer defaultLeadTimeDays
) {
}
