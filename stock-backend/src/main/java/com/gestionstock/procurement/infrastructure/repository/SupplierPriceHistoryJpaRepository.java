package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.infrastructure.entity.SupplierPriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPriceHistoryJpaRepository extends JpaRepository<SupplierPriceHistoryEntity, Long> {
    List<SupplierPriceHistoryEntity> findTop20ByOrganisationIdAndProductIdOrderByObservedAtDesc(Long organisationId, Long productId);
}
