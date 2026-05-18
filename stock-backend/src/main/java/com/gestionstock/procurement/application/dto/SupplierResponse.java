package com.gestionstock.procurement.application.dto;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        Long organisationId,
        String name,
        String email,
        String phone,
        Integer leadTimeDays,
        Boolean active,
        LocalDateTime createdAt
) {
}
