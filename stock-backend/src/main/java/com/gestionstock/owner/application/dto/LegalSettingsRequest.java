package com.gestionstock.owner.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LegalSettingsRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 160, message = "Company name must be shorter than 160 characters")
        String companyName,
        String legalNotice,
        String privacyPolicy,
        String terms,
        String legalDocumentUrl,
        String privacyDocumentUrl,
        String termsDocumentUrl
) {
}
