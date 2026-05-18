package com.gestionstock.admin.application.dto;

public record ExecutiveTimelineActionResponse(
        String priority,
        String title,
        String description,
        String route
) {
}
