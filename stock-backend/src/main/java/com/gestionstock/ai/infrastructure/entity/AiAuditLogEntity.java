package com.gestionstock.ai.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ai_audit_logs")
public class AiAuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "actor_email", nullable = false, length = 180)
    private String actorEmail;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 40)
    private String source;

    @Column(nullable = false, length = 1200)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
