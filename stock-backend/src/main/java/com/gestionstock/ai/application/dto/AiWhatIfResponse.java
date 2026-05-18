package com.gestionstock.ai.application.dto;

public record AiWhatIfResponse(
        Long productId,
        String productName,
        int currentStock,
        int orderQuantity,
        int leadTimeDays,
        double averageDailyDemand,
        double currentCoverageDays,
        double projectedCoverageDays,
        int projectedStockAfterLeadTime,
        String recommendation
) {
}
