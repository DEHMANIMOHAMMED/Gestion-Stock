package com.gestionstock.sales.domain.model;

import java.time.LocalDateTime;

public record SalesDemand(
        Long productId,
        Integer quantity,
        LocalDateTime soldAt
) {}
