package com.gestionstock.stock.application.mapper;

import com.gestionstock.stock.application.dto.StockResponse;
import com.gestionstock.stock.domain.model.Stock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockMapper {

    StockResponse toResponse(Stock stock);
}
