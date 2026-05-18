package com.gestionstock.procurement.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderRequest(
        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        LocalDate expectedDeliveryDate,

        @Valid
        @NotEmpty(message = "Purchase order must contain at least one line")
        List<PurchaseOrderLineRequest> lines
) {
}
