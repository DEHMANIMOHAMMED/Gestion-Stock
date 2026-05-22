package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;

public record SaleLineResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
