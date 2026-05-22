package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;

public record DemandForecastAlertResponse(
        Long productId,
        String productName,
        String sku,
        String severity,
        BigDecimal variancePercent,
        String message,
        String recommendedAction
) {}
