package com.gestionstock.procurement.application.dto;

import java.math.BigDecimal;

public record PurchaseOrderLineResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        Integer quantity,
        Integer receivedQuantity,
        BigDecimal unitCost
) {
}
