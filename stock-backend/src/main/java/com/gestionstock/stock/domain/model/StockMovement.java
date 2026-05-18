package com.gestionstock.stock.domain.model;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record StockMovement(
        Long id,
        Long organisationId,
        Long productId,
        Integer quantity,
        MovementType type,
        LocalDateTime createdAt
) {}
