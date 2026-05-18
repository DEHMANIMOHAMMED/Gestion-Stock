package com.gestionstock.stock.application.dto;

public record StockResponse(
        Long productId,
        Integer quantity
) {}
