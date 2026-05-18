package com.gestionstock.notification.domain.service;

import com.gestionstock.audit.AuditLogService;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.notification.domain.model.AdminNotification;
import com.gestionstock.notification.domain.model.AdminNotificationPreferences;
import com.gestionstock.notification.infrastructure.entity.AdminNotificationActionEntity;
import com.gestionstock.notification.infrastructure.entity.AdminNotificationEntity;
import com.gestionstock.notification.infrastructure.entity.AdminNotificationPreferencesEntity;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationActionJpaRepository;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationJpaRepository;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationPreferencesJpaRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationJpaRepository repository;
    private final AdminNotificationActionJpaRepository actionRepository;
    private final AdminNotificationPreferencesJpaRepository preferencesRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AuditLogService auditLogService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public List<AdminNotification> latest() {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        return repository.findTop20ByOrganisationIdOrderByCreatedAtDesc(organisationId).stream()
                .map(this::toDomain)
                .toList();
    }

    public List<AdminNotification> history(String type, String severity, String readStatus) {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        return repository.findByOrganisationIdOrderByCreatedAtDesc(organisationId).stream()
                .filter(notification -> type == null || type.isBlank() || "ALL".equals(type) || notification.getType().equals(type))
                .filter(notification -> severity == null || severity.isBlank() || "ALL".equals(severity) || notification.getSeverity().equals(severity))
                .filter(notification -> readStatus == null || readStatus.isBlank() || "ALL".equals(readStatus)
                        || ("UNREAD".equals(readStatus) && notification.getReadAt() == null)
                        || ("READ".equals(readStatus) && notification.getReadAt() != null))
                .map(this::toDomain)
                .toList();
    }

    public AdminNotificationPreferences preferences() {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        return preferencesRepository.findByOrganisationId(organisationId)
                .map(this::toPreferencesDomain)
                .orElseGet(() -> defaultPreferences(organisationId));
    }

    @Transactional
    public AdminNotificationPreferences updatePreferences(
            boolean thresholdNotificationsEnabled,
            boolean criticalStockoutNotificationsEnabled
    ) {
        Long organisationId = TenantContext.requireOrganisationId();
        User user = authenticatedUserProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only admins can update notification preferences");
        }
        AdminNotificationPreferences existing = preferences();
        AdminNotificationPreferences saved = toPreferencesDomain(preferencesRepository.save(AdminNotificationPreferencesEntity.builder()
                .id(existing.id())
                .organisationId(organisationId)
                .thresholdNotificationsEnabled(thresholdNotificationsEnabled)
                .criticalStockoutNotificationsEnabled(criticalStockoutNotificationsEnabled)
                .updatedAt(LocalDateTime.now())
                .updatedByUserId(user.getId())
                .build()));
        auditLogService.record(user, "NOTIFICATION_PREFERENCES_UPDATED", "ADMIN_NOTIFICATION_PREFERENCES", saved.id(), "BACKEND",
                "Preferences notifications admin mises a jour");
        return saved;
    }

    public long unreadCount() {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        return repository.countByOrganisationIdAndReadAtIsNull(organisationId);
    }

    @Transactional
    public void markRead(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        AdminNotificationEntity notification = repository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setReadAt(LocalDateTime.now());
        AdminNotificationEntity saved = repository.save(notification);
        auditLogService.record(authenticatedUserProvider.requireUser(), "NOTIFICATION_READ", "ADMIN_NOTIFICATION", saved.getId(), "BACKEND",
                "Notification marquee comme lue: " + saved.getTitle());
    }

    public AdminNotification get(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        return repository.findByIdAndOrganisationId(id, organisationId)
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    }

    @Transactional
    public AdminNotification markActioned(Long id, String action) {
        Long organisationId = TenantContext.requireOrganisationId();
        User user = authenticatedUserProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only admins can action notifications");
        }
        AdminNotificationEntity notification = repository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setStatus("ACTIONED");
        notification.setActionTaken(action);
        notification.setActionedAt(LocalDateTime.now());
        notification.setActionedByUserId(user.getId());
        notification.setReadAt(LocalDateTime.now());
        AdminNotificationEntity saved = repository.save(notification);
        recordAction(saved, action, null, user.getId());
        auditLogService.record(user, "NOTIFICATION_ACTIONED", "ADMIN_NOTIFICATION", saved.getId(), "BACKEND",
                "Action notification: " + action);
        return toDomain(saved);
    }

    @Transactional
    public AdminNotification dismiss(Long id, String reason) {
        Long organisationId = TenantContext.requireOrganisationId();
        User user = authenticatedUserProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only admins can dismiss notifications");
        }
        AdminNotificationEntity notification = repository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if ("CRITICAL".equals(notification.getSeverity()) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Critical notifications require a dismissal reason");
        }
        notification.setStatus("DISMISSED");
        notification.setActionTaken("DISMISS");
        notification.setActionedAt(LocalDateTime.now());
        notification.setActionedByUserId(user.getId());
        notification.setDismissalReason(reason == null ? null : reason.trim());
        notification.setReadAt(LocalDateTime.now());
        AdminNotificationEntity saved = repository.save(notification);
        recordAction(saved, "DISMISS", saved.getDismissalReason(), user.getId());
        auditLogService.record(user, "NOTIFICATION_DISMISSED", "ADMIN_NOTIFICATION", saved.getId(), "BACKEND",
                "Notification ignoree: " + saved.getTitle());
        return toDomain(saved);
    }

    public List<AdminNotificationActionEntity> actions(Long notificationId) {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        repository.findByIdAndOrganisationId(notificationId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        return actionRepository.findByNotificationIdAndOrganisationIdOrderByCreatedAtDesc(notificationId, organisationId);
    }

    @Transactional
    public void createOnce(
            Long organisationId,
            String type,
            String severity,
            String title,
            String message,
            Long purchaseOrderId,
            Long supplierId,
            String deduplicationKey
    ) {
        AdminNotificationPreferences preferences = preferencesRepository.findByOrganisationId(organisationId)
                .map(this::toPreferencesDomain)
                .orElseGet(() -> defaultPreferences(organisationId));
        if ("PURCHASE_ORDER_THRESHOLD".equals(type) && !preferences.thresholdNotificationsEnabled()) {
            return;
        }
        if ("AI_CRITICAL_STOCKOUT".equals(type) && !preferences.criticalStockoutNotificationsEnabled()) {
            return;
        }
        if (repository.findByOrganisationIdAndDeduplicationKey(organisationId, deduplicationKey).isPresent()) {
            return;
        }
        AdminNotificationEntity saved = repository.save(AdminNotificationEntity.builder()
                .organisationId(organisationId)
                .type(type)
                .severity(severity)
                .title(title)
                .message(message)
                .purchaseOrderId(purchaseOrderId)
                .supplierId(supplierId)
                .deduplicationKey(deduplicationKey)
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .build());
        auditLogService.record(organisationId, null, "system", "NOTIFICATION_CREATED", "ADMIN_NOTIFICATION", saved.getId(), "BACKEND",
                severity + " " + type + ": " + title);
    }

    public SseEmitter stream() {
        Long organisationId = TenantContext.requireOrganisationId();
        requireAdmin();
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> emitLoop(emitter, organisationId));
        return emitter;
    }

    private void emitLoop(SseEmitter emitter, Long organisationId) {
        long lastUnreadCount = -1;
        try {
            while (true) {
                long unreadCount = repository.countByOrganisationIdAndReadAtIsNull(organisationId);
                if (unreadCount != lastUnreadCount) {
                    emitter.send(SseEmitter.event()
                            .name("notifications")
                            .data(new NotificationStreamPayload(
                                    unreadCount,
                                    repository.findTop20ByOrganisationIdAndReadAtIsNullOrderByCreatedAtDesc(organisationId)
                                            .stream()
                                            .map(this::toDomain)
                                            .toList()
                            )));
                    lastUnreadCount = unreadCount;
                } else {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ok"));
                }
                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException ignored) {
            emitter.complete();
            Thread.currentThread().interrupt();
        }
    }

    private void requireAdmin() {
        if (authenticatedUserProvider.requireUser().getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only admins can access notifications");
        }
    }

    private void recordAction(AdminNotificationEntity notification, String action, String reason, Long actorUserId) {
        actionRepository.save(AdminNotificationActionEntity.builder()
                .notificationId(notification.getId())
                .organisationId(notification.getOrganisationId())
                .actionCode(action)
                .reason(reason)
                .actorUserId(actorUserId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private AdminNotification toDomain(AdminNotificationEntity entity) {
        return AdminNotification.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .type(entity.getType())
                .severity(entity.getSeverity())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .purchaseOrderId(entity.getPurchaseOrderId())
                .supplierId(entity.getSupplierId())
                .deduplicationKey(entity.getDeduplicationKey())
                .readAt(entity.getReadAt())
                .status(entity.getStatus())
                .actionTaken(entity.getActionTaken())
                .actionedAt(entity.getActionedAt())
                .actionedByUserId(entity.getActionedByUserId())
                .dismissalReason(entity.getDismissalReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private AdminNotificationPreferences defaultPreferences(Long organisationId) {
        return AdminNotificationPreferences.builder()
                .organisationId(organisationId)
                .thresholdNotificationsEnabled(true)
                .criticalStockoutNotificationsEnabled(true)
                .updatedAt(null)
                .updatedByUserId(null)
                .build();
    }

    private AdminNotificationPreferences toPreferencesDomain(AdminNotificationPreferencesEntity entity) {
        return AdminNotificationPreferences.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .thresholdNotificationsEnabled(entity.isThresholdNotificationsEnabled())
                .criticalStockoutNotificationsEnabled(entity.isCriticalStockoutNotificationsEnabled())
                .updatedAt(entity.getUpdatedAt())
                .updatedByUserId(entity.getUpdatedByUserId())
                .build();
    }

    public record NotificationStreamPayload(long unreadCount, List<AdminNotification> notifications) {
    }
}
