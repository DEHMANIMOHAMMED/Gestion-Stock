package com.gestionstock.ai.application.dto;

import java.time.LocalDateTime;

public record AiRunResponse(
        Long id,
        String status,
        String triggerType,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage,
        Integer forecastsCount,
        Integer risksCount,
        Integer recommendationsCount,
        Integer anomaliesCount,
        Integer insightsCount,
        String modelSource
) {
}
