package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiRunRepository extends JpaRepository<AiRunEntity, Long> {
    List<AiRunEntity> findTop10ByOrganisationIdOrderByStartedAtDesc(Long organisationId);

    Optional<AiRunEntity> findTopByOrganisationIdOrderByStartedAtDesc(Long organisationId);

    boolean existsByOrganisationIdAndStatusIn(Long organisationId, List<String> statuses);
}
