package com.gestionstock.notification.application.dto;

import java.time.LocalDateTime;

public record AdminNotificationActionResponse(
        Long id,
        String action,
        String reason,
        Long actorUserId,
        LocalDateTime createdAt
) {
}
