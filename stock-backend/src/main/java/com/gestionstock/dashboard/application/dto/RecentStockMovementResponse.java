package com.gestionstock.dashboard.application.dto;

import java.time.LocalDateTime;

public record RecentStockMovementResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        String type,
        LocalDateTime createdAt
) {
}
