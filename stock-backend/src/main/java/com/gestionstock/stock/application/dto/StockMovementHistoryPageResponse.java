package com.gestionstock.stock.application.dto;

import java.util.List;

public record StockMovementHistoryPageResponse(
        List<StockMovementHistoryResponse> items,
        long total,
        int page,
        int size
) {}
