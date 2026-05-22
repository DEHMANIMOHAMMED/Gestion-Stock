package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record DemandSeasonalityResponse(
        String industry,
        String pattern,
        BigDecimal seasonalityIndex,
        List<SalesPeriodPointResponse> weekdaySeries,
        String insight
) {}
