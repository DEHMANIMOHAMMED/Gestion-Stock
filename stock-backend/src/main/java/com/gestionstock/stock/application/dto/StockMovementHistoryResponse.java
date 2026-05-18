package com.gestionstock.stock.application.dto;

import java.time.LocalDateTime;

public record StockMovementHistoryResponse(
        Long id,
        Long productId,
        Integer quantity,
        String type,
        LocalDateTime createdAt
) {}
