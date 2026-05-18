package com.gestionstock.ai.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiCopilotMessageResponse(
        Long id,
        String question,
        String answer,
        String source,
        List<AiCitationResponse> citations,
        LocalDateTime createdAt
) {
}
