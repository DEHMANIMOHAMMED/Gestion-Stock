package com.gestionstock.procurement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductSupplierResponse(
        Long id,
        Long organisationId,
        Long productId,
        String productName,
        String sku,
        Long supplierId,
        String supplierName,
        Integer supplierLeadTimeDays,
        BigDecimal unitCost,
        Integer minimumOrderQuantity,
        Boolean preferred,
        Double supplierScore,
        Double onTimeRate,
        Double conformityRate,
        String scoreExplanation,
        Boolean active,
        LocalDateTime createdAt
) {
}
