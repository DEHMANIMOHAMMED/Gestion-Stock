package com.gestionstock.owner.application.dto;

public record OwnerSupportSubscriptionResponse(
        Long organisationId,
        String planCode,
        String status,
        boolean cancelAtPeriodEnd
) {
}
