package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiAnomalyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiAnomalyRepository extends JpaRepository<AiAnomalyEntity, Long> {
    List<AiAnomalyEntity> findByOrganisationIdOrderByDetectedAtDesc(Long organisationId);

    void deleteByOrganisationId(Long organisationId);
}
