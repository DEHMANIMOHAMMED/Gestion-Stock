package com.gestionstock.ai.application.dto;

public record AiCitationResponse(
        String type,
        String label,
        Long productId,
        Long supplierId,
        Long purchaseOrderId
) {
}
