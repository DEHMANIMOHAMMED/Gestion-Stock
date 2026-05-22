package com.gestionstock.sales.infrastructure.repository;

import com.gestionstock.sales.infrastructure.entity.SaleLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleLineJpaRepository extends JpaRepository<SaleLineEntity, Long> {

    List<SaleLineEntity> findBySaleId(Long saleId);

    List<SaleLineEntity> findBySaleIdIn(List<Long> saleIds);

    @Query("""
            select line.productId as productId, line.quantity as quantity, sale.soldAt as soldAt
            from SaleLineEntity line
            join SaleEntity sale on sale.id = line.saleId
            where line.organisationId = :organisationId
              and sale.organisationId = :organisationId
              and sale.status = com.gestionstock.sales.domain.model.SaleStatus.COMPLETED
              and sale.soldAt >= :since
            """)
    List<SalesDemandProjection> findDemandSince(
            @Param("organisationId") Long organisationId,
            @Param("since") LocalDateTime since
    );
}
