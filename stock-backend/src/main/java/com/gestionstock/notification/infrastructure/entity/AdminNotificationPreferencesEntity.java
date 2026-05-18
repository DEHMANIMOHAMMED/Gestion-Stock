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
@Table(name = "admin_notification_preferences")
public class AdminNotificationPreferencesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false, unique = true)
    private Long organisationId;

    @Column(name = "threshold_notifications_enabled", nullable = false)
    private boolean thresholdNotificationsEnabled;

    @Column(name = "critical_stockout_notifications_enabled", nullable = false)
    private boolean criticalStockoutNotificationsEnabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;
}
