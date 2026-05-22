package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        String reference,
        String customerName,
        String channel,
        String status,
        BigDecimal totalAmount,
        LocalDateTime soldAt,
        List<SaleLineResponse> lines
) {}
