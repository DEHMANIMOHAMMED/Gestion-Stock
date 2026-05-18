package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiInsightRepository extends JpaRepository<AiInsightEntity, Long> {
    List<AiInsightEntity> findByOrganisationIdOrderByGeneratedAtDesc(Long organisationId);

    void deleteByOrganisationId(Long organisationId);
}
