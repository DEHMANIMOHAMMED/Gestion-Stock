package com.gestionstock.ai.application.dto;

public record AiProductHealthResponse(
        Long productId,
        String productName,
        String sku,
        String category,
        int currentStock,
        int minStock,
        double averageDailyDemand,
        int forecast30Days,
        double stockCoverageDays,
        double riskScore,
        String riskLevel,
        int recommendedQuantity,
        Long recommendationId,
        Long purchaseOrderId,
        Long supplierId,
        String supplierName,
        Double supplierScore,
        int openPurchaseOrders,
        double healthScore,
        String actionRecommendation
) {
}
