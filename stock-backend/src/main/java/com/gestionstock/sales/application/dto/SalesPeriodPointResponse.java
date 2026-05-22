package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesPeriodPointResponse(
        LocalDate date,
        BigDecimal revenue,
        Integer unitsSold,
        Integer salesCount
) {}
