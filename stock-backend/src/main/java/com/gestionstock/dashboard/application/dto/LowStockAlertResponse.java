package com.gestionstock.dashboard.application.dto;

public record LowStockAlertResponse(
        Long productId,
        String sku,
        String name,
        Integer quantity,
        Integer minStock,
        Integer missingQuantity
) {
}
