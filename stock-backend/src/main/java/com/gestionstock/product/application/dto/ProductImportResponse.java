package com.gestionstock.product.application.dto;

import java.util.List;

public record ProductImportResponse(
        int createdProducts,
        int updatedProducts,
        int stockMovementsCreated,
        List<String> errors
) {
}
