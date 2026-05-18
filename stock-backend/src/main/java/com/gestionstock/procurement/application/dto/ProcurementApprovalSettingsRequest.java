package com.gestionstock.procurement.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProcurementApprovalSettingsRequest(
        @NotNull @DecimalMin("0.00") BigDecimal approvalThreshold
) {
}
