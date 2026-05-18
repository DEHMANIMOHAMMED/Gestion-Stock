package com.gestionstock.admin.application.dto;

public record SystemHealthServiceResponse(
        String name,
        String status,
        long latencyMs,
        String detail
) {
}
