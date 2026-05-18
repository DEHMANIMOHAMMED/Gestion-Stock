package com.gestionstock.product.application.dto;

public record ProductResponse(
        Long id,
        Long organisationId,
        String name,
        String sku,
        String category,
        Integer minStock,
        String unit
) {}
