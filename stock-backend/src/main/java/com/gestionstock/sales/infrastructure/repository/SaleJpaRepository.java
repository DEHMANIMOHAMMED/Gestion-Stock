package com.gestionstock.sales.infrastructure.repository;

import com.gestionstock.sales.infrastructure.entity.SaleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleJpaRepository extends JpaRepository<SaleEntity, Long> {

    Optional<SaleEntity> findByIdAndOrganisationId(Long id, Long organisationId);

    Page<SaleEntity> findByOrganisationId(Long organisationId, Pageable pageable);

    List<SaleEntity> findByOrganisationIdAndSoldAtBetweenOrderBySoldAtDesc(
            Long organisationId,
            LocalDateTime from,
            LocalDateTime to
    );

    boolean existsByOrganisationIdAndReference(Long organisationId, String reference);
}
