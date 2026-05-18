package com.gestionstock.ai.application.dto;

public record AiCopilotActionResponse(
        String type,
        String label,
        String description,
        Long productId,
        Long recommendationId,
        Long purchaseOrderId,
        Long supplierId,
        Integer quantity,
        Integer leadTimeDays,
        String route,
        boolean requiresAdminConfirmation
) {
}
