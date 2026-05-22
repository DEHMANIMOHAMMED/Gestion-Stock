package com.gestionstock.sales.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record Sale(
        Long id,
        Long organisationId,
        String reference,
        String customerName,
        SalesChannel channel,
        SaleStatus status,
        BigDecimal totalAmount,
        LocalDateTime soldAt,
        LocalDateTime createdAt,
        List<SaleLine> lines
) {}
