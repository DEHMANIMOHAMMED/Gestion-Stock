package com.gestionstock.procurement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record PurchaseOrderApprovalItem(
        Long id,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        BigDecimal orderTotal,
        Integer linesCount,
        Integer totalQuantity,
        BigDecimal maxRiskScore,
        String maxRiskLevel,
        LocalDate earliestStockoutDate,
        String urgency,
        String riskReason,
        LocalDate expectedDeliveryDate,
        LocalDateTime createdAt
) {
}
