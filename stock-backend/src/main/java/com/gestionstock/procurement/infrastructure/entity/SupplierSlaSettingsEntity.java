package com.gestionstock.procurement.infrastructure.entity;

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
@Table(name = "supplier_sla_settings")
public class SupplierSlaSettingsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "target_lead_time_days", nullable = false)
    private Integer targetLeadTimeDays;

    @Column(name = "target_conformity_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetConformityRate;

    @Column(name = "target_on_time_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetOnTimeRate;

    @Column(length = 500)
    private String notes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
