package com.gestionstock.ai.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiForecastBacktestResponse(
        Long productId,
        String productName,
        String sku,
        Integer horizonDays,
        BigDecimal mae,
        BigDecimal mape,
        BigDecimal reliabilityScore,
        String qualityLevel,
        String recommendation,
        List<AiForecastBacktestPointResponse> points
) {}
