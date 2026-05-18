package com.gestionstock.procurement.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record Supplier360Response(
        Long id,
        String name,
        String email,
        String phone,
        Integer leadTimeDays,
        Boolean active,
        Integer totalOrders,
        Integer draftOrders,
        Integer orderedOrders,
        Integer receivedOrders,
        Integer lateOrders,
        Integer coveredProducts,
        Integer orderedQuantity,
        Integer receivedQuantity,
        BigDecimal totalSpend,
        BigDecimal averageUnitCost,
        Double onTimeRate,
        Double conformityRate,
        Double averageDelayDays,
        Double healthScore,
        List<Supplier360ProductResponse> products,
        List<Supplier360OrderResponse> recentOrders,
        List<String> recommendations
) {
}
