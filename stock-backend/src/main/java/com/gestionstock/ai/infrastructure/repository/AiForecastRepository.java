package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiForecastRepository extends JpaRepository<AiForecastEntity, Long> {
    List<AiForecastEntity> findByOrganisationIdOrderByGeneratedAtDesc(Long organisationId);

    List<AiForecastEntity> findByOrganisationIdAndHorizonDaysOrderByGeneratedAtDesc(Long organisationId, Integer horizonDays);

    void deleteByOrganisationId(Long organisationId);
}
