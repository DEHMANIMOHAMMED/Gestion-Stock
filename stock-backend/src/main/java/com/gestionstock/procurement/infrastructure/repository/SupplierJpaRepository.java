package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.infrastructure.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierJpaRepository extends JpaRepository<SupplierEntity, Long> {
    List<SupplierEntity> findByOrganisationIdOrderByNameAsc(Long organisationId);

    Optional<SupplierEntity> findByIdAndOrganisationId(Long id, Long organisationId);

    boolean existsByNameAndOrganisationId(String name, Long organisationId);
}
