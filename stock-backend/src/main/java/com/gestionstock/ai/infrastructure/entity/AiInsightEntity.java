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
@Table(name = "ai_insights")
public class AiInsightEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "insight_type", nullable = false, length = 40)
    private String insightType;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
