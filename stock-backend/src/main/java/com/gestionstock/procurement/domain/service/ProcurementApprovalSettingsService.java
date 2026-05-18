package com.gestionstock.procurement.domain.service;

import com.gestionstock.audit.AuditLogService;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.procurement.domain.model.ProcurementApprovalSettings;
import com.gestionstock.procurement.domain.repository.ProcurementApprovalSettingsRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProcurementApprovalSettingsService {

    private final ProcurementApprovalSettingsRepository repository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AuditLogService auditLogService;

    @Value("${stockpilot.procurement.approval-threshold:1000}")
    private BigDecimal defaultApprovalThreshold;

    public ProcurementApprovalSettings currentSettings() {
        Long organisationId = TenantContext.requireOrganisationId();
        return repository.findByOrganisationId(organisationId)
                .orElseGet(() -> ProcurementApprovalSettings.builder()
                        .organisationId(organisationId)
                        .approvalThreshold(defaultApprovalThreshold)
                        .updatedAt(null)
                        .updatedByUserId(null)
                        .build());
    }

    public BigDecimal currentThreshold() {
        return currentSettings().approvalThreshold();
    }

    public boolean isDefaultValue(ProcurementApprovalSettings settings) {
        return settings.id() == null;
    }

    @Transactional
    public ProcurementApprovalSettings updateThreshold(BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Approval threshold must be positive");
        }
        User user = authenticatedUserProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only admins can update approval settings");
        }
        Long organisationId = TenantContext.requireOrganisationId();
        Optional<ProcurementApprovalSettings> existing = repository.findByOrganisationId(organisationId);

        ProcurementApprovalSettings saved = repository.save(ProcurementApprovalSettings.builder()
                .id(existing.map(ProcurementApprovalSettings::id).orElse(null))
                .organisationId(organisationId)
                .approvalThreshold(threshold)
                .updatedAt(LocalDateTime.now())
                .updatedByUserId(user.getId())
                .build());
        auditLogService.record(user, "APPROVAL_THRESHOLD_UPDATED", "PROCUREMENT_APPROVAL_SETTINGS", saved.id(), "BACKEND",
                "Seuil approbation commande mis a jour a " + threshold);
        return saved;
    }
}
