package com.gestionstock.sales.infrastructure.entity;

import com.gestionstock.sales.domain.model.SaleStatus;
import com.gestionstock.sales.domain.model.SalesChannel;
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
@Table(name = "sales")
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "customer_name", length = 160)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SalesChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SaleStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "sold_at", nullable = false)
    private LocalDateTime soldAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
