package com.gestionstock.notification.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "admin_notifications")
public class AdminNotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "deduplication_key", nullable = false, length = 180)
    private String deduplicationKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "action_taken", length = 60)
    private String actionTaken;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;

    @Column(name = "actioned_by_user_id")
    private Long actionedByUserId;

    @Column(name = "dismissal_reason", length = 600)
    private String dismissalReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
