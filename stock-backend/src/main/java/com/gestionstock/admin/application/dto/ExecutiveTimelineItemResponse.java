package com.gestionstock.admin.application.dto;

import java.time.LocalDateTime;

public record ExecutiveTimelineItemResponse(
        String type,
        String severity,
        int priorityScore,
        String title,
        String description,
        LocalDateTime occurredAt,
        Long notificationId,
        Long productId,
        Long purchaseOrderId,
        Long recommendationId,
        Long supplierId
) {
}
