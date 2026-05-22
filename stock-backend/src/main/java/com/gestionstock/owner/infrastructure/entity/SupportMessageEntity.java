package com.gestionstock.owner.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "support_messages")
public class SupportMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sender_email", nullable = false, length = 180)
    private String senderEmail;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "attachment_name", length = 220)
    private String attachmentName;

    @Column(name = "attachment_content_type", length = 120)
    private String attachmentContentType;

    @Column(name = "attachment_data", columnDefinition = "TEXT")
    private String attachmentData;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "OPEN";
        }
    }
}
