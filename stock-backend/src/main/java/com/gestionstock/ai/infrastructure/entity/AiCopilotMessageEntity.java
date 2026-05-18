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
@Table(name = "ai_copilot_messages")
public class AiCopilotMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false, length = 4000)
    private String answer;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(length = 3000)
    private String citations;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
