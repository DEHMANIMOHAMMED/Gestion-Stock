package com.gestionstock.ai.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AiStockoutRiskResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        LocalDate estimatedStockoutDate,
        BigDecimal riskScore,
        String riskLevel,
        String reason,
        LocalDateTime generatedAt
) {
}
