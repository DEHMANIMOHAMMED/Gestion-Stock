package com.gestionstock.procurement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ProductSupplier(
        Long id,
        Long organisationId,
        Long productId,
        Long supplierId,
        BigDecimal unitCost,
        Integer minimumOrderQuantity,
        Boolean preferred,
        Boolean active,
        LocalDateTime createdAt
) {
}
