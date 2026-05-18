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
@Table(name = "ai_runs")
public class AiRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "forecasts_count", nullable = false)
    private Integer forecastsCount;

    @Column(name = "risks_count", nullable = false)
    private Integer risksCount;

    @Column(name = "recommendations_count", nullable = false)
    private Integer recommendationsCount;

    @Column(name = "anomalies_count", nullable = false)
    private Integer anomaliesCount;

    @Column(name = "insights_count", nullable = false)
    private Integer insightsCount;

    @Column(name = "model_source", nullable = false, length = 40)
    private String modelSource;
}
