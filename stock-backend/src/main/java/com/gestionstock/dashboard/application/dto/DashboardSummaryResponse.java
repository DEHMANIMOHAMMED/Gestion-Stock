package com.gestionstock.dashboard.application.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long totalProducts,
        long totalUnits,
        long lowStockProducts,
        long outOfStockProducts,
        List<RecentStockMovementResponse> recentMovements
) {
}
