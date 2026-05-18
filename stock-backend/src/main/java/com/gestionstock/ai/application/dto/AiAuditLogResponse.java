package com.gestionstock.ai.application.dto;

import java.time.LocalDateTime;

public record AiAuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String targetType,
        Long targetId,
        String source,
        String summary,
        LocalDateTime createdAt
) {
}
