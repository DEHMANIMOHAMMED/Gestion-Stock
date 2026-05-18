package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderJpaRepository extends JpaRepository<PurchaseOrderEntity, Long> {
    @EntityGraph(attributePaths = "lines")
    List<PurchaseOrderEntity> findByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseOrderEntity> findByIdAndOrganisationId(Long id, Long organisationId);
}
