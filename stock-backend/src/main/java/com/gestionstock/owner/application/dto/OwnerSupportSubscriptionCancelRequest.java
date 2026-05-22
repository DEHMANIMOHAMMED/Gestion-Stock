package com.gestionstock.owner.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerSupportSubscriptionCancelRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}
