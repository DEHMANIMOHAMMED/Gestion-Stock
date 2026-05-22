package com.gestionstock.sales.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record SaleLine(
        Long id,
        Long saleId,
        Long organisationId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
