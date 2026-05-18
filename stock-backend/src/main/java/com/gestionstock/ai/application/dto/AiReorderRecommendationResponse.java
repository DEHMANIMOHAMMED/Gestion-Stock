package com.gestionstock.ai.application.dto;

import java.time.LocalDateTime;

public record AiReorderRecommendationResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        Integer recommendedQuantity,
        Integer leadTimeDays,
        Integer safetyStock,
        String reason,
        String status,
        Long purchaseOrderId,
        Long preferredSupplierId,
        String preferredSupplierName,
        Integer preferredSupplierLeadTimeDays,
        java.math.BigDecimal preferredSupplierUnitCost,
        Integer preferredSupplierMinimumOrderQuantity,
        Double preferredSupplierScore,
        Double preferredSupplierOnTimeRate,
        Double preferredSupplierConformityRate,
        String preferredSupplierScoreExplanation,
        LocalDateTime generatedAt
) {
}
