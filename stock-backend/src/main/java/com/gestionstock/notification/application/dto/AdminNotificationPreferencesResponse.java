package com.gestionstock.notification.application.dto;

import java.time.LocalDateTime;

public record AdminNotificationPreferencesResponse(
        boolean thresholdNotificationsEnabled,
        boolean criticalStockoutNotificationsEnabled,
        LocalDateTime updatedAt,
        Long updatedByUserId,
        boolean defaultValue
) {
}
