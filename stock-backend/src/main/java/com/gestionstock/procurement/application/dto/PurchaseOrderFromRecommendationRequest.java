package com.gestionstock.procurement.application.dto;

public record PurchaseOrderFromRecommendationRequest(
        Long recommendationId,

        Long supplierId
) {
}
