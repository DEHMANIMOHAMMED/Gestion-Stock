package com.gestionstock.procurement.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SupplierSlaRequest(
        @NotNull Long supplierId,
        @NotNull @Min(0) Integer targetLeadTimeDays,
        @NotNull @DecimalMin("0.00") BigDecimal targetOnTimeRate,
        @NotNull @DecimalMin("0.00") BigDecimal targetConformityRate,
        String notes
) {
}
