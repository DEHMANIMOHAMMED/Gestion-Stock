package com.gestionstock.admin.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ModelRegistryResponse(
        LocalDateTime generatedAt,
        long totalForecasts,
        long modelsCount,
        BigDecimal averageErrorPercent,
        int productsToRecalibrate,
        String leaderModel,
        List<ModelRegistryModelResponse> models
) {}
