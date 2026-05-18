package com.gestionstock.notification.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminNotificationPreferences(
        Long id,
        Long organisationId,
        boolean thresholdNotificationsEnabled,
        boolean criticalStockoutNotificationsEnabled,
        LocalDateTime updatedAt,
        Long updatedByUserId
) {
}
