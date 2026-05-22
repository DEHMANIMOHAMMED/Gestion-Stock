package com.gestionstock.owner.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SupportMessageResponse(
        Long id,
        Long organisationId,
        String organisationName,
        Long userId,
        String senderEmail,
        String subject,
        String message,
        String attachmentName,
        String attachmentContentType,
        String attachmentData,
        String status,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        LocalDateTime resolvedAt,
        List<SupportReplyResponse> replies
) {
}
