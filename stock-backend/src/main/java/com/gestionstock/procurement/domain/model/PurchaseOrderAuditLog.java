package com.gestionstock.procurement.domain.model;

import com.gestionstock.iam.infrastructure.entity.Role;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PurchaseOrderAuditLog(
        Long id,
        Long organisationId,
        Long purchaseOrderId,
        String action,
        PurchaseOrderStatus previousStatus,
        PurchaseOrderStatus newStatus,
        Long actorUserId,
        String actorEmail,
        Role actorRole,
        BigDecimal orderTotal,
        LocalDateTime createdAt
) {
}
