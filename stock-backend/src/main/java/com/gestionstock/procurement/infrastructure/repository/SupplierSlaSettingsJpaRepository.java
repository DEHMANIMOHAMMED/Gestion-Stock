package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.infrastructure.entity.SupplierSlaSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierSlaSettingsJpaRepository extends JpaRepository<SupplierSlaSettingsEntity, Long> {
    List<SupplierSlaSettingsEntity> findByOrganisationId(Long organisationId);

    Optional<SupplierSlaSettingsEntity> findByOrganisationIdAndSupplierId(Long organisationId, Long supplierId);
}
