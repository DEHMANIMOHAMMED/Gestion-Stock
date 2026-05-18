package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.domain.model.PurchaseOrderAuditLog;
import com.gestionstock.procurement.domain.repository.PurchaseOrderAuditLogRepository;
import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderAuditLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PurchaseOrderAuditLogRepositoryImpl implements PurchaseOrderAuditLogRepository {

    private final PurchaseOrderAuditLogJpaRepository jpaRepository;

    @Override
    public PurchaseOrderAuditLog save(PurchaseOrderAuditLog auditLog) {
        return toDomain(jpaRepository.save(toEntity(auditLog)));
    }

    @Override
    public List<PurchaseOrderAuditLog> findByPurchaseOrder(Long purchaseOrderId, Long organisationId) {
        return jpaRepository.findByPurchaseOrderIdAndOrganisationIdOrderByCreatedAtAsc(purchaseOrderId, organisationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private PurchaseOrderAuditLog toDomain(PurchaseOrderAuditLogEntity entity) {
        return PurchaseOrderAuditLog.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .purchaseOrderId(entity.getPurchaseOrderId())
                .action(entity.getAction())
                .previousStatus(entity.getPreviousStatus())
                .newStatus(entity.getNewStatus())
                .actorUserId(entity.getActorUserId())
                .actorEmail(entity.getActorEmail())
                .actorRole(entity.getActorRole())
                .orderTotal(entity.getOrderTotal())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PurchaseOrderAuditLogEntity toEntity(PurchaseOrderAuditLog domain) {
        return PurchaseOrderAuditLogEntity.builder()
                .id(domain.id())
                .organisationId(domain.organisationId())
                .purchaseOrderId(domain.purchaseOrderId())
                .action(domain.action())
                .previousStatus(domain.previousStatus())
                .newStatus(domain.newStatus())
                .actorUserId(domain.actorUserId())
                .actorEmail(domain.actorEmail())
                .actorRole(domain.actorRole())
                .orderTotal(domain.orderTotal())
                .createdAt(domain.createdAt())
                .build();
    }
}
