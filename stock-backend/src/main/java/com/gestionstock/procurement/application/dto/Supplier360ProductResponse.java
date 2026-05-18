package com.gestionstock.procurement.application.dto;

import java.math.BigDecimal;

public record Supplier360ProductResponse(
        Long productId,
        String productName,
        String sku,
        BigDecimal unitCost,
        Integer minimumOrderQuantity,
        Boolean preferred,
        Double supplierScore,
        String scoreExplanation
) {
}
