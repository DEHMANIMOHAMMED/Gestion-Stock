package com.gestionstock.procurement.domain.repository;

import com.gestionstock.procurement.domain.model.PurchaseOrder;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository {
    PurchaseOrder save(PurchaseOrder purchaseOrder);

    List<PurchaseOrder> findAll(Long organisationId);

    Optional<PurchaseOrder> findById(Long id, Long organisationId);
}
