package com.gestionstock.admin.application.dto;

import java.math.BigDecimal;

public record ModelRegistryProductResponse(
        Long productId,
        String productName,
        String sku,
        Integer horizonDays,
        BigDecimal predictedQuantity,
        BigDecimal movingAverageError,
        BigDecimal seasonalError,
        BigDecimal fastapiError,
        String reason
) {}
