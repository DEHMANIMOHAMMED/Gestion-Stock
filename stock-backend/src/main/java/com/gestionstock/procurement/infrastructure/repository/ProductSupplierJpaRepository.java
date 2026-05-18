package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.infrastructure.entity.ProductSupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSupplierJpaRepository extends JpaRepository<ProductSupplierEntity, Long> {
    List<ProductSupplierEntity> findByOrganisationIdAndProductIdAndActiveTrue(Long organisationId, Long productId);

    List<ProductSupplierEntity> findByOrganisationIdOrderByProductIdAscSupplierIdAsc(Long organisationId);

    Optional<ProductSupplierEntity> findByOrganisationIdAndProductIdAndSupplierId(Long organisationId, Long productId, Long supplierId);
}
