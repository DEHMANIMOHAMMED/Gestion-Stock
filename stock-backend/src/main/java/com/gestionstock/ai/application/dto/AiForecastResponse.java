package com.gestionstock.ai.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiForecastResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        Integer horizonDays,
        BigDecimal predictedQuantity,
        BigDecimal confidenceScore,
        String modelName,
        LocalDateTime generatedAt
) {
}
