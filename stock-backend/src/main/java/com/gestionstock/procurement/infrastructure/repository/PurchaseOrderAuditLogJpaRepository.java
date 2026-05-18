package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderAuditLogJpaRepository extends JpaRepository<PurchaseOrderAuditLogEntity, Long> {
    List<PurchaseOrderAuditLogEntity> findByPurchaseOrderIdAndOrganisationIdOrderByCreatedAtAsc(
            Long purchaseOrderId,
            Long organisationId
    );
}
