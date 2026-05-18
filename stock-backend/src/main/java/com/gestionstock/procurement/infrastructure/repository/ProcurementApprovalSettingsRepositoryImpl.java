package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.domain.model.ProcurementApprovalSettings;
import com.gestionstock.procurement.domain.repository.ProcurementApprovalSettingsRepository;
import com.gestionstock.procurement.infrastructure.entity.ProcurementApprovalSettingsEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProcurementApprovalSettingsRepositoryImpl implements ProcurementApprovalSettingsRepository {

    private final ProcurementApprovalSettingsJpaRepository jpaRepository;

    @Override
    public ProcurementApprovalSettings save(ProcurementApprovalSettings settings) {
        return toDomain(jpaRepository.save(toEntity(settings)));
    }

    @Override
    public Optional<ProcurementApprovalSettings> findByOrganisationId(Long organisationId) {
        return jpaRepository.findByOrganisationId(organisationId).map(this::toDomain);
    }

    private ProcurementApprovalSettings toDomain(ProcurementApprovalSettingsEntity entity) {
        return ProcurementApprovalSettings.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .approvalThreshold(entity.getApprovalThreshold())
                .updatedAt(entity.getUpdatedAt())
                .updatedByUserId(entity.getUpdatedByUserId())
                .build();
    }

    private ProcurementApprovalSettingsEntity toEntity(ProcurementApprovalSettings domain) {
        return ProcurementApprovalSettingsEntity.builder()
                .id(domain.id())
                .organisationId(domain.organisationId())
                .approvalThreshold(domain.approvalThreshold())
                .updatedAt(domain.updatedAt())
                .updatedByUserId(domain.updatedByUserId())
                .build();
    }
}
