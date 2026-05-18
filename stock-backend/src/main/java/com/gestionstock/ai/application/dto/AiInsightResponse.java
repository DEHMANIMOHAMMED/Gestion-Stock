package com.gestionstock.ai.application.dto;

import java.time.LocalDateTime;

public record AiInsightResponse(
        Long id,
        String title,
        String content,
        String insightType,
        String priority,
        LocalDateTime generatedAt
) {
}
