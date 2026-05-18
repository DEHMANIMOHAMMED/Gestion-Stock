package com.gestionstock.stock.infrastructure.repository;

import com.gestionstock.stock.infrastructure.entity.StockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface StockJpaRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findByProductIdAndOrganisationId(Long productId, Long organisationId);

    List<StockEntity> findByOrganisationId(Long organisationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from StockEntity s
            where s.productId = :productId
              and s.organisationId = :organisationId
            """)
    Optional<StockEntity> findByProductIdAndOrganisationIdForUpdate(
            @Param("productId") Long productId,
            @Param("organisationId") Long organisationId
    );
}
