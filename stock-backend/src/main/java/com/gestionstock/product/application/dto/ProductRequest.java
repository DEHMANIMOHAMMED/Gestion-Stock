package com.gestionstock.product.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ProductRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "SKU is required")
        @Size(max = 64, message = "SKU must be at most 64 characters")
        String sku,

        String category,

        @NotNull(message = "Minimum stock is required")
        @Min(value = 0, message = "Minimum stock must be zero or greater")
        Integer minStock,

        String unit
) {}
