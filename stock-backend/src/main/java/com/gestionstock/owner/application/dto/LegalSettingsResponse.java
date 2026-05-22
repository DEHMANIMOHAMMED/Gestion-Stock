package com.gestionstock.owner.application.dto;

import java.time.LocalDateTime;

public record LegalSettingsResponse(
        Long id,
        String companyName,
        String legalNotice,
        String privacyPolicy,
        String terms,
        String legalDocumentUrl,
        String privacyDocumentUrl,
        String termsDocumentUrl,
        LocalDateTime updatedAt,
        String updatedByEmail
) {
}
