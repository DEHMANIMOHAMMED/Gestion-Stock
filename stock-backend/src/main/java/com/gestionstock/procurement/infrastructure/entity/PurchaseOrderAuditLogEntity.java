package com.gestionstock.procurement.infrastructure.entity;

import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
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
@Table(name = "purchase_order_audit_logs")
public class PurchaseOrderAuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(nullable = false, length = 40)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private PurchaseOrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private PurchaseOrderStatus newStatus;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", nullable = false, length = 180)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 20)
    private Role actorRole;

    @Column(name = "order_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal orderTotal;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
