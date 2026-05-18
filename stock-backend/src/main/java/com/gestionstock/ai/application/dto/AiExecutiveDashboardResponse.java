package com.gestionstock.ai.application.dto;

import com.gestionstock.procurement.application.dto.PurchaseOrderResponse;

import java.util.List;

public record AiExecutiveDashboardResponse(
        int totalProducts,
        int criticalStockCount,
        int highRiskCount,
        int pendingRecommendationsCount,
        int pendingPurchaseOrdersCount,
        int supplierIssuesCount,
        double stockHealthAverage,
        List<AiProductHealthResponse> topRisks,
        List<AiReorderRecommendationResponse> recommendations,
        List<PurchaseOrderResponse> pendingOrders,
        List<SupplierIssueResponse> supplierIssues,
        List<AiInsightResponse> insights
) {
}
