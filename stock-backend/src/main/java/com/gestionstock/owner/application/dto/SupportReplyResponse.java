package com.gestionstock.owner.application.dto;

import java.time.LocalDateTime;

public record SupportReplyResponse(
        Long id,
        Long authorUserId,
        String authorEmail,
        String authorRole,
        String message,
        String attachmentName,
        String attachmentContentType,
        String attachmentData,
        LocalDateTime createdAt
) {
}
