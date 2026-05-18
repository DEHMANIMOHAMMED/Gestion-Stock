package com.gestionstock.notification.application.dto;

import java.util.List;

public record AdminNotificationStreamResponse(
        long unreadCount,
        List<AdminNotificationResponse> notifications
) {
}
