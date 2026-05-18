package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiReorderRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiReorderRecommendationRepository extends JpaRepository<AiReorderRecommendationEntity, Long> {
    List<AiReorderRecommendationEntity> findByOrganisationIdOrderByRecommendedQuantityDesc(Long organisationId);

    Optional<AiReorderRecommendationEntity> findByIdAndOrganisationId(Long id, Long organisationId);

    void deleteByOrganisationId(Long organisationId);
}
