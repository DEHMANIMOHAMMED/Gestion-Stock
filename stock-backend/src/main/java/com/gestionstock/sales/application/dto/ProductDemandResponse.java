package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;

public record ProductDemandResponse(
        Long productId,
        String productName,
        String sku,
        Integer quantitySold,
        BigDecimal revenue
) {}
