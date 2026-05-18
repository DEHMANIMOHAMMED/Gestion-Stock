package com.gestionstock.notification.application.dto;

public record AdminNotificationPreferencesRequest(
        boolean thresholdNotificationsEnabled,
        boolean criticalStockoutNotificationsEnabled
) {
}
