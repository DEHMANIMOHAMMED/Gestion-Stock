package com.gestionstock.notification.presentation.controller;

import com.gestionstock.notification.application.dto.AdminNotificationPreferencesRequest;
import com.gestionstock.notification.application.dto.AdminNotificationPreferencesResponse;
import com.gestionstock.notification.application.dto.AdminNotificationActionResponse;
import com.gestionstock.notification.application.dto.AdminNotificationResponse;
import com.gestionstock.notification.application.dto.NotificationActionRequest;
import com.gestionstock.notification.domain.model.AdminNotification;
import com.gestionstock.notification.domain.model.AdminNotificationPreferences;
import com.gestionstock.notification.domain.service.AdminNotificationService;
import com.gestionstock.procurement.domain.service.PurchaseOrderService;
import com.gestionstock.security.PlanAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService service;
    private final PurchaseOrderService purchaseOrderService;
    private final PlanAccessService planAccessService;

    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> latest() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(service.latest().stream().map(this::toResponse).toList());
    }

    @GetMapping("/history")
    public ResponseEntity<List<AdminNotificationResponse>> history(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String readStatus
    ) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(service.history(type, severity, readStatus).stream().map(this::toResponse).toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(Map.of("unreadCount", service.unreadCount()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        planAccessService.requireProPlan();
        service.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<AdminNotificationResponse> action(
            @PathVariable Long id,
            @Valid @RequestBody NotificationActionRequest request
    ) {
        planAccessService.requireProPlan();
        AdminNotification notification = service.get(id);
        return switch (request.action()) {
            case "APPROVE_ORDER" -> {
                requireLinkedOrder(notification);
                purchaseOrderService.approve(notification.purchaseOrderId());
                yield ResponseEntity.ok(toResponse(service.markActioned(id, request.action())));
            }
            case "CONFIRM_ORDER" -> {
                requireLinkedOrder(notification);
                purchaseOrderService.confirm(notification.purchaseOrderId());
                yield ResponseEntity.ok(toResponse(service.markActioned(id, request.action())));
            }
            case "DISMISS" -> ResponseEntity.ok(toResponse(service.dismiss(id, request.reason())));
            default -> throw new IllegalArgumentException("Unsupported notification action");
        };
    }

    @GetMapping("/{id}/actions")
    public ResponseEntity<List<AdminNotificationActionResponse>> actions(@PathVariable Long id) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(service.actions(id).stream()
                .map(action -> new AdminNotificationActionResponse(
                        action.getId(),
                        action.getActionCode(),
                        action.getReason(),
                        action.getActorUserId(),
                        action.getCreatedAt()
                ))
                .toList());
    }

    @GetMapping("/preferences")
    public ResponseEntity<AdminNotificationPreferencesResponse> preferences() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(toPreferencesResponse(service.preferences()));
    }

    @PutMapping("/preferences")
    public ResponseEntity<AdminNotificationPreferencesResponse> updatePreferences(
            @RequestBody AdminNotificationPreferencesRequest request
    ) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(toPreferencesResponse(service.updatePreferences(
                request.thresholdNotificationsEnabled(),
                request.criticalStockoutNotificationsEnabled()
        )));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        planAccessService.requireProPlan();
        return service.stream();
    }

    private AdminNotificationResponse toResponse(AdminNotification notification) {
        return new AdminNotificationResponse(
                notification.id(),
                notification.type(),
                notification.severity(),
                notification.title(),
                notification.message(),
                notification.purchaseOrderId(),
                notification.supplierId(),
                notification.readAt(),
                notification.status(),
                notification.actionTaken(),
                notification.actionedAt(),
                notification.actionedByUserId(),
                notification.dismissalReason(),
                notification.createdAt()
        );
    }

    private void requireLinkedOrder(AdminNotification notification) {
        if (notification.purchaseOrderId() == null) {
            throw new IllegalArgumentException("Notification is not linked to a purchase order");
        }
    }

    private AdminNotificationPreferencesResponse toPreferencesResponse(AdminNotificationPreferences preferences) {
        return new AdminNotificationPreferencesResponse(
                preferences.thresholdNotificationsEnabled(),
                preferences.criticalStockoutNotificationsEnabled(),
                preferences.updatedAt(),
                preferences.updatedByUserId(),
                preferences.id() == null
        );
    }
}
