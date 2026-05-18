package com.gestionstock.audit;

import com.gestionstock.ai.infrastructure.entity.AiAuditLogEntity;
import com.gestionstock.ai.infrastructure.repository.AiAuditLogRepository;
import com.gestionstock.iam.infrastructure.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AiAuditLogRepository auditLogRepository;

    public void record(User user, String action, String targetType, Long targetId, String source, String summary) {
        if (user == null || user.getOrganisation() == null) {
            return;
        }
        record(user.getOrganisation().getId(), user.getId(), user.getEmail(), action, targetType, targetId, source, summary);
    }

    public void record(Long organisationId, Long userId, String actorEmail, String action, String targetType, Long targetId, String source, String summary) {
        if (organisationId == null) {
            return;
        }
        auditLogRepository.save(AiAuditLogEntity.builder()
                .organisationId(organisationId)
                .userId(userId)
                .actorEmail(actorEmail == null || actorEmail.isBlank() ? "system" : actorEmail)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .source(source)
                .summary(truncate(summary == null ? "-" : summary, 1100))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
