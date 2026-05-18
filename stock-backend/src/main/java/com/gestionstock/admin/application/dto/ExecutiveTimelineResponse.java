package com.gestionstock.admin.application.dto;

import java.time.LocalDate;
import java.util.List;

public record ExecutiveTimelineResponse(
        LocalDate date,
        int criticalRisks,
        int openNotifications,
        int pendingOrders,
        int pendingRecommendations,
        String executiveSummary,
        List<String> keyDecisions,
        List<ExecutiveTimelineItemResponse> items,
        List<ExecutiveTimelineActionResponse> actions
) {
}
