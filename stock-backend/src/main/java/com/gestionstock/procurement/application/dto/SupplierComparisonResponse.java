package com.gestionstock.procurement.application.dto;

import java.math.BigDecimal;

public record SupplierComparisonResponse(
        Long productId,
        String productName,
        String sku,
        Long supplierId,
        String supplierName,
        BigDecimal unitCost,
        Integer minimumOrderQuantity,
        Integer leadTimeDays,
        Double supplierScore,
        Double onTimeRate,
        Double conformityRate,
        Boolean preferred,
        Boolean bestAlternative,
        String recommendation
) {
}
