package com.gestionstock.ai.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiAnomalyResponse(
        Long id,
        Long productId,
        String productName,
        Long stockMovementId,
        String anomalyType,
        String severity,
        BigDecimal score,
        String explanation,
        LocalDateTime detectedAt
) {
}
