package com.gestionstock.ai.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiCopilotRequest(
        Long conversationId,
        @NotBlank @Size(max = 800) String question
) {
}
