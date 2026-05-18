package com.gestionstock.ai.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ai_anomalies")
public class AiAnomalyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "stock_movement_id")
    private Long stockMovementId;

    @Column(name = "anomaly_type", nullable = false, length = 40)
    private String anomalyType;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;
}
