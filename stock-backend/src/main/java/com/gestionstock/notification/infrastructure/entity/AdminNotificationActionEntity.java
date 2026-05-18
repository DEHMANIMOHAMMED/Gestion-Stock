package com.gestionstock.notification.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notification_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "action_code", nullable = false, length = 60)
    private String actionCode;

    @Column(length = 600)
    private String reason;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
