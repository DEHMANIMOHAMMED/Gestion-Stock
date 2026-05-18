package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderLine;
import com.gestionstock.procurement.domain.repository.PurchaseOrderRepository;
import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderEntity;
import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderLineEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepository {

    private final PurchaseOrderJpaRepository jpaRepository;

    @Override
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        PurchaseOrderEntity entity = toEntity(purchaseOrder);
        entity.getLines().forEach(line -> line.setPurchaseOrder(entity));
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<PurchaseOrder> findAll(Long organisationId) {
        return jpaRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<PurchaseOrder> findById(Long id, Long organisationId) {
        return jpaRepository.findByIdAndOrganisationId(id, organisationId).map(this::toDomain);
    }

    private PurchaseOrder toDomain(PurchaseOrderEntity entity) {
        return PurchaseOrder.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .supplierId(entity.getSupplierId())
                .status(entity.getStatus())
                .expectedDeliveryDate(entity.getExpectedDeliveryDate())
                .createdAt(entity.getCreatedAt())
                .receivedAt(entity.getReceivedAt())
                .lines(entity.getLines().stream().map(this::toDomain).toList())
                .build();
    }

    private PurchaseOrderLine toDomain(PurchaseOrderLineEntity entity) {
        return PurchaseOrderLine.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .receivedQuantity(entity.getReceivedQuantity())
                .unitCost(entity.getUnitCost())
                .build();
    }

    private PurchaseOrderEntity toEntity(PurchaseOrder domain) {
        return PurchaseOrderEntity.builder()
                .id(domain.id())
                .organisationId(domain.organisationId())
                .supplierId(domain.supplierId())
                .status(domain.status())
                .expectedDeliveryDate(domain.expectedDeliveryDate())
                .createdAt(domain.createdAt())
                .receivedAt(domain.receivedAt())
                .lines(domain.lines().stream().map(this::toEntity).toList())
                .build();
    }

    private PurchaseOrderLineEntity toEntity(PurchaseOrderLine domain) {
        return PurchaseOrderLineEntity.builder()
                .id(domain.id())
                .productId(domain.productId())
                .quantity(domain.quantity())
                .receivedQuantity(domain.receivedQuantity())
                .unitCost(domain.unitCost())
                .build();
    }
}
