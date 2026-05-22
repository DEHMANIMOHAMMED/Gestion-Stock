package com.gestionstock.ai.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ai_forecast_snapshots")
public class AiForecastSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "horizon_days", nullable = false)
    private Integer horizonDays;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "predicted_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal predictedQuantity;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "model_name", nullable = false, length = 80)
    private String modelName;

    @Column(name = "selected_model", length = 80)
    private String selectedModel;

    @Column(name = "model_selection_reason")
    private String modelSelectionReason;

    @Column(name = "moving_average_error", precision = 8, scale = 2)
    private BigDecimal movingAverageError;

    @Column(name = "seasonal_error", precision = 8, scale = 2)
    private BigDecimal seasonalError;

    @Column(name = "fastapi_error", precision = 8, scale = 2)
    private BigDecimal fastapiError;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
