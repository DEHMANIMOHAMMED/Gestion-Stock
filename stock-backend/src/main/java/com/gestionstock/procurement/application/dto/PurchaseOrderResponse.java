package com.gestionstock.procurement.application.dto;

import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        Long organisationId,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDate expectedDeliveryDate,
        LocalDateTime createdAt,
        LocalDateTime receivedAt,
        List<PurchaseOrderLineResponse> lines
) {
}
