package com.gestionstock.sales.application.dto;

import java.util.List;

public record DemandAnalyticsResponse(
        SalesSummaryResponse summary,
        List<SalesPeriodPointResponse> dailySeries,
        List<SalesChannelResponse> channels,
        List<ProductDemandTrendResponse> topRisingProducts,
        List<ProductDemandTrendResponse> topDecliningProducts,
        List<ProductDemandTrendResponse> highDemandLowStockProducts,
        DemandSeasonalityResponse seasonality,
        List<DemandForecastComparisonResponse> forecastComparisons,
        List<DemandForecastAlertResponse> forecastAlerts
) {}
