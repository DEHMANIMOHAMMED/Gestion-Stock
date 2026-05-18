package com.gestionstock.ai.application.dto;

import java.util.List;

public record AiCopilotResponse(
        Long conversationId,
        String answer,
        List<String> bullets,
        List<Long> relatedProductIds,
        List<AiCitationResponse> citations,
        String source,
        List<AiCopilotActionResponse> actions
) {
}
