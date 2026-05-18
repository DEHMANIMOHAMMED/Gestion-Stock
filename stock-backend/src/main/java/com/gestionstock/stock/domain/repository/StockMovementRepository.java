package com.gestionstock.stock.domain.repository;

import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.StockMovement;

import java.util.List;

public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    List<StockMovement> findHistory(Long organisationId, Long productId, MovementType type, int page, int size);

    List<StockMovement> findRecent(Long organisationId, int limit);

    long countHistory(Long organisationId, Long productId, MovementType type);
}
