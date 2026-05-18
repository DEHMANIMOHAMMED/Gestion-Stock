package com.gestionstock.procurement.application.dto;

import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseOrderAuditLogResponse(
        Long id,
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
