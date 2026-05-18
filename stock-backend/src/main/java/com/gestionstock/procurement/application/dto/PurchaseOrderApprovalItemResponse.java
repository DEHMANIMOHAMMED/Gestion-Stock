package com.gestionstock.procurement.application.dto;

import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseOrderApprovalItemResponse(
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
