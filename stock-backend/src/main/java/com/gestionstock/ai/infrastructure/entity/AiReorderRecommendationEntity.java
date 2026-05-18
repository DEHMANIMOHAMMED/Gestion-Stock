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
@Table(name = "ai_reorder_recommendations")
public class AiReorderRecommendationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "recommended_quantity", nullable = false)
    private Integer recommendedQuantity;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
