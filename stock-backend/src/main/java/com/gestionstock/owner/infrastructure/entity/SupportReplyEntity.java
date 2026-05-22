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
@Table(name = "support_replies")
public class SupportReplyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "support_message_id", nullable = false)
    private Long supportMessageId;

    @Column(name = "author_user_id")
    private Long authorUserId;

    @Column(name = "author_email", nullable = false, length = 180)
    private String authorEmail;

    @Column(name = "author_role", nullable = false, length = 20)
    private String authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "attachment_name", length = 220)
    private String attachmentName;

    @Column(name = "attachment_content_type", length = 120)
    private String attachmentContentType;

    @Column(name = "attachment_data", columnDefinition = "TEXT")
    private String attachmentData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
