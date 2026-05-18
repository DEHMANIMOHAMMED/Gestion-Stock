package com.gestionstock.product.domain.model;

import lombok.Builder;

@Builder
public record Product(
        Long id,
        Long organisationId,
        String name,
        String sku,
        String category,
        Integer minStock,
        String unit
) {}
