package com.gestionstock.stock.domain.model;

import lombok.Builder;

@Builder
public record Stock(
        Long id,
        Long organisationId,
        Long productId,
        Integer quantity
) {}
