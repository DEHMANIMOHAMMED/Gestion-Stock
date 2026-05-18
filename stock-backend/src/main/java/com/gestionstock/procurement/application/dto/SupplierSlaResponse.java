package com.gestionstock.procurement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplierSlaResponse(
        Long supplierId,
        String supplierName,
        Integer targetLeadTimeDays,
        BigDecimal targetOnTimeRate,
        BigDecimal targetConformityRate,
        String notes,
        LocalDateTime updatedAt
) {
}
