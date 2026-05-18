package com.gestionstock.ai.application.dto;

import java.util.List;

public record AiDashboardResponse(
        List<AiForecastResponse> forecasts,
        List<AiStockoutRiskResponse> stockoutRisks,
        List<AiReorderRecommendationResponse> reorderRecommendations,
        List<AiAnomalyResponse> anomalies,
        List<AiInsightResponse> insights
) {
}
