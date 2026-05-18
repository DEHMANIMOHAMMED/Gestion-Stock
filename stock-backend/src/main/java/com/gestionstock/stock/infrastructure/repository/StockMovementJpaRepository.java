package com.gestionstock.stock.infrastructure.repository;

import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.infrastructure.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, Long> {

    Page<StockMovementEntity> findByOrganisationId(Long organisationId, Pageable pageable);

    Page<StockMovementEntity> findByOrganisationIdAndProductId(
            Long organisationId,
            Long productId,
            Pageable pageable
    );

    Page<StockMovementEntity> findByOrganisationIdAndType(
            Long organisationId,
            MovementType type,
            Pageable pageable
    );

    Page<StockMovementEntity> findByOrganisationIdAndProductIdAndType(
            Long organisationId,
            Long productId,
            MovementType type,
            Pageable pageable
    );

    long countByOrganisationId(Long organisationId);

    long countByOrganisationIdAndProductId(Long organisationId, Long productId);

    long countByOrganisationIdAndType(Long organisationId, MovementType type);

    long countByOrganisationIdAndProductIdAndType(Long organisationId, Long productId, MovementType type);
}
