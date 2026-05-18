package com.gestionstock.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StockMovementRequest(

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        Integer quantity,

        @NotBlank(message = "Movement type is required")
        @Pattern(regexp = "IN|OUT|ADJUST", message = "Movement type must be IN, OUT or ADJUST")
        String type // IN, OUT, ADJUST
) {}
