package com.gestionstock.admin.application.dto;

import com.gestionstock.ai.application.dto.AiRunResponse;

import java.time.LocalDateTime;
import java.util.List;

public record SystemHealthResponse(
        String overallStatus,
        LocalDateTime checkedAt,
        List<SystemHealthServiceResponse> services,
        AiRunResponse latestAiRun,
        long unreadAdminNotifications,
        long productsCount,
        long stocksCount,
        long purchaseOrdersCount
) {
}
