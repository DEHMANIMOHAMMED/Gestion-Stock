package com.gestionstock.stock.application.mapper;

import com.gestionstock.stock.application.dto.StockMovementHistoryResponse;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.StockMovement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    StockMovementHistoryResponse toHistoryResponse(StockMovement movement);

    default String map(MovementType type) {
        return type == null ? null : type.name();
    }
}
