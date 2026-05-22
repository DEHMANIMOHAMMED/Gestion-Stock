package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiForecastSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiForecastSnapshotRepository extends JpaRepository<AiForecastSnapshotEntity, Long> {
    List<AiForecastSnapshotEntity> findByOrganisationIdAndProductIdAndHorizonDaysOrderByGeneratedAtDesc(
            Long organisationId,
            Long productId,
            Integer horizonDays
    );

    void deleteByOrganisationId(Long organisationId);
}
