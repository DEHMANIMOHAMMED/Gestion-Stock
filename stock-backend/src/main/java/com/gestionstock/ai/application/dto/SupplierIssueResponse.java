package com.gestionstock.ai.application.dto;

public record SupplierIssueResponse(
        Long supplierId,
        String supplierName,
        int leadTimeDays,
        long openOrders,
        double issueScore,
        String reason
) {
}
