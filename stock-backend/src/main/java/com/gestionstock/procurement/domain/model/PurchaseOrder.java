package com.gestionstock.procurement.domain.model;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PurchaseOrder(
        Long id,
        Long organisationId,
        Long supplierId,
        PurchaseOrderStatus status,
        LocalDate expectedDeliveryDate,
        LocalDateTime createdAt,
        LocalDateTime receivedAt,
        List<PurchaseOrderLine> lines
) {
}
