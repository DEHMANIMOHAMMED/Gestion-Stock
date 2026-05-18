package com.gestionstock.procurement.application.dto;

import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Supplier360OrderResponse(
        Long id,
        PurchaseOrderStatus status,
        LocalDate expectedDeliveryDate,
        LocalDateTime createdAt,
        LocalDateTime receivedAt,
        Integer orderedQuantity,
        Integer receivedQuantity,
        BigDecimal totalCost
) {
}
