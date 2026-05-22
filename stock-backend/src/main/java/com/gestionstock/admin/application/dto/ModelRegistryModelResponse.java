package com.gestionstock.admin.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record ModelRegistryModelResponse(
        String modelName,
        long usageCount,
        BigDecimal usageSharePercent,
        BigDecimal averageErrorPercent,
        int productsToRecalibrate,
        String status,
        List<ModelRegistryProductResponse> winningProducts,
        List<ModelRegistryProductResponse> recalibrationProducts
) {}
