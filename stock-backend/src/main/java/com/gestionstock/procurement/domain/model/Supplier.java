package com.gestionstock.procurement.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record Supplier(
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
