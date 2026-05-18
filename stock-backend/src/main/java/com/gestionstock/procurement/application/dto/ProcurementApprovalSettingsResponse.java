package com.gestionstock.procurement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProcurementApprovalSettingsResponse(
        BigDecimal approvalThreshold,
        LocalDateTime updatedAt,
        Long updatedByUserId,
        boolean defaultValue
) {
}
