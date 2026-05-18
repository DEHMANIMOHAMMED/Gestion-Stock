package com.gestionstock.procurement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PurchaseOrderLine(
        Long id,
        Long productId,
        Integer quantity,
        Integer receivedQuantity,
        BigDecimal unitCost
) {
}
