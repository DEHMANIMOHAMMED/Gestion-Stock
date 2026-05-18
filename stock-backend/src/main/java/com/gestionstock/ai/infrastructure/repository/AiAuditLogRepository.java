package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiAuditLogRepository extends JpaRepository<AiAuditLogEntity, Long> {
    List<AiAuditLogEntity> findTop100ByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    List<AiAuditLogEntity> findTop500ByOrganisationIdOrderByCreatedAtDesc(Long organisationId);
}
