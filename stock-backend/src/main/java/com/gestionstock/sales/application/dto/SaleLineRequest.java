package com.gestionstock.sales.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleLineRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) {}
