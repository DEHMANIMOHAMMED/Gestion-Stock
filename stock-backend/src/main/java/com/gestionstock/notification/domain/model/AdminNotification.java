package com.gestionstock.notification.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminNotification(
        Long id,
        Long organisationId,
        String type,
        String severity,
        String title,
        String message,
        Long purchaseOrderId,
        Long supplierId,
        String deduplicationKey,
        LocalDateTime readAt,
        String status,
        String actionTaken,
        LocalDateTime actionedAt,
        Long actionedByUserId,
        String dismissalReason,
        LocalDateTime createdAt
) {
}
