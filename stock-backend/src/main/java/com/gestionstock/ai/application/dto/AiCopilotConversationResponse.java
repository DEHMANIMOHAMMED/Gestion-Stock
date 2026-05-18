package com.gestionstock.ai.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiCopilotConversationResponse(
        Long id,
        String title,
        LocalDateTime updatedAt,
        List<AiCopilotMessageResponse> messages
) {
}
