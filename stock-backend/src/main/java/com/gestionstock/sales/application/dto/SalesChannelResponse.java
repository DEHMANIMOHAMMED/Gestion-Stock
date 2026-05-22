package com.gestionstock.sales.application.dto;

import java.math.BigDecimal;

public record SalesChannelResponse(
        String channel,
        BigDecimal revenue,
        Integer unitsSold,
        Integer salesCount,
        BigDecimal sharePercent
) {}
