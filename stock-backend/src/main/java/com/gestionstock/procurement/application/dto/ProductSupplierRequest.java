package com.gestionstock.procurement.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductSupplierRequest(
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "Supplier ID is required") Long supplierId,
        @DecimalMin(value = "0.0", message = "Unit cost must be positive") BigDecimal unitCost,
        @NotNull(message = "Minimum order quantity is required")
        @Min(value = 1, message = "Minimum order quantity must be positive") Integer minimumOrderQuantity,
        Boolean preferred
) {
}
