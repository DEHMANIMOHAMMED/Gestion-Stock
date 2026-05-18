package com.gestionstock.procurement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ProcurementApprovalSettings(
        Long id,
        Long organisationId,
        BigDecimal approvalThreshold,
        LocalDateTime updatedAt,
        Long updatedByUserId
) {
}
