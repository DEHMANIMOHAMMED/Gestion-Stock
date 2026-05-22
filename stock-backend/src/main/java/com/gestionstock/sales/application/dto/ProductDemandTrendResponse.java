package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;

public record ProductDemandTrendResponse(
        Long productId,
        String productName,
        String sku,
        Integer currentPeriodUnits,
        Integer previousPeriodUnits,
        BigDecimal trendPercent,
        BigDecimal revenue,
        Integer currentStock,
        Integer minStock,
        String signal
) {}
