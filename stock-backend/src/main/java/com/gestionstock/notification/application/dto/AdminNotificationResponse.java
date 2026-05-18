package com.gestionstock.notification.application.dto;

import java.time.LocalDateTime;

public record AdminNotificationResponse(
        Long id,
        String type,
        String severity,
        String title,
        String message,
        Long purchaseOrderId,
        Long supplierId,
        LocalDateTime readAt,
        String status,
        String actionTaken,
        LocalDateTime actionedAt,
        Long actionedByUserId,
        String dismissalReason,
        LocalDateTime createdAt
) {
}
