package com.gestionstock.sales.domain.repository;

import com.gestionstock.sales.domain.model.Sale;
import com.gestionstock.sales.domain.model.SalesDemand;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesRepository {

    Sale save(Sale sale);

    Optional<Sale> findById(Long id, Long organisationId);

    List<Sale> findRecent(Long organisationId, int limit);

    List<Sale> findBetween(Long organisationId, LocalDateTime from, LocalDateTime to);

    List<SalesDemand> findDemandSince(Long organisationId, LocalDateTime since);
}
