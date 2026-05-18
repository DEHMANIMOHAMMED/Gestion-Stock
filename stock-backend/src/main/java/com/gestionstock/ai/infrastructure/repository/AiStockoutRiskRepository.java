package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiStockoutRiskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiStockoutRiskRepository extends JpaRepository<AiStockoutRiskEntity, Long> {
    List<AiStockoutRiskEntity> findByOrganisationIdOrderByRiskScoreDesc(Long organisationId);

    void deleteByOrganisationId(Long organisationId);
}
