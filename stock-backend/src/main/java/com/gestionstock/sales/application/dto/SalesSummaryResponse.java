package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalesSummaryResponse(
        BigDecimal revenue30Days,
        Integer unitsSold30Days,
        Integer salesCount30Days,
        BigDecimal averageBasket,
        List<ProductDemandResponse> topProducts
) {}
