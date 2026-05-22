package com.gestionstock.ai.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiForecastBacktestPointResponse(
        LocalDate date,
        int actualUnits,
        BigDecimal predictedUnits,
        BigDecimal variancePercent
) {}
