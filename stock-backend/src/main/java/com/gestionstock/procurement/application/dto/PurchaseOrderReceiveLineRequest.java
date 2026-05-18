package com.gestionstock.procurement.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseOrderReceiveLineRequest(
        @NotNull(message = "Line ID is required") Long lineId,
        @NotNull(message = "Received quantity is required")
        @Min(value = 1, message = "Received quantity must be positive") Integer quantity
) {
}
