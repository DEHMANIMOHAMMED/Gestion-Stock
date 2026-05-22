package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;

public record DemandForecastComparisonResponse(
        Long productId,
        String productName,
        String sku,
        int actualUnits30Days,
        BigDecimal forecastUnits30Days,
        BigDecimal variancePercent,
        BigDecimal confidenceScore,
        String status,
        String recommendation
) {}
