package com.gestionstock.procurement.domain.repository;

import com.gestionstock.procurement.domain.model.PurchaseOrderAuditLog;

import java.util.List;

public interface PurchaseOrderAuditLogRepository {
    PurchaseOrderAuditLog save(PurchaseOrderAuditLog auditLog);

    List<PurchaseOrderAuditLog> findByPurchaseOrder(Long purchaseOrderId, Long organisationId);
}
