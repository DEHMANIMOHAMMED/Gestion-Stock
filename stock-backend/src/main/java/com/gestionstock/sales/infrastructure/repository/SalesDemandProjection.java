package com.gestionstock.sales.infrastructure.repository;

import java.time.LocalDateTime;

public interface SalesDemandProjection {
    Long getProductId();

    Integer getQuantity();

    LocalDateTime getSoldAt();
}
