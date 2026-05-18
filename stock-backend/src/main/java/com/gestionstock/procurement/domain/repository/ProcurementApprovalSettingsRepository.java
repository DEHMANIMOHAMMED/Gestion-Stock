package com.gestionstock.procurement.domain.repository;

import com.gestionstock.procurement.domain.model.ProcurementApprovalSettings;

import java.util.Optional;

public interface ProcurementApprovalSettingsRepository {
    ProcurementApprovalSettings save(ProcurementApprovalSettings settings);

    Optional<ProcurementApprovalSettings> findByOrganisationId(Long organisationId);
}
