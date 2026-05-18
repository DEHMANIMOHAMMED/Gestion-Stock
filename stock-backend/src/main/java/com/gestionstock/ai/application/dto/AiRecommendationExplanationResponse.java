package com.gestionstock.ai.application.dto;

import java.util.List;

public record AiRecommendationExplanationResponse(
        Long recommendationId,
        String summary,
        List<String> drivers,
        List<String> risks,
        String nextAction,
        List<AiCitationResponse> citations,
        String source
) {
}
