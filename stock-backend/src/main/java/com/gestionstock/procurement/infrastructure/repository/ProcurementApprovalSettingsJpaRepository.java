package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.infrastructure.entity.ProcurementApprovalSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcurementApprovalSettingsJpaRepository extends JpaRepository<ProcurementApprovalSettingsEntity, Long> {
    Optional<ProcurementApprovalSettingsEntity> findByOrganisationId(Long organisationId);
}
