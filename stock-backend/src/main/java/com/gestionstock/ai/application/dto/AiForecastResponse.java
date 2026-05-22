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
        String confidenceLevel,
        BigDecimal backtestErrorPercent,
        BigDecimal demandTrendPercent,
        Integer salesVolume30Days,
        String demandSignal,
        String modelName,
        String selectedModel,
        String modelSelectionReason,
        BigDecimal movingAverageError,
        BigDecimal seasonalError,
        BigDecimal fastapiError,
        LocalDateTime generatedAt
) {
}
