package com.gestionstock.stock.domain.repository;

import com.gestionstock.stock.domain.model.Stock;

import java.util.Optional;
import java.util.List;

public interface StockRepository {

    Optional<Stock> findByProduct(Long productId, Long orgId);

    Optional<Stock> findByProductForUpdate(Long productId, Long orgId);

    List<Stock> findAll(Long orgId);

    Stock save(Stock stock);
}
