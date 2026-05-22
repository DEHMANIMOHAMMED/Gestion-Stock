package com.gestionstock.owner.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerSupportPasswordRequest(
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 120, message = "Password must contain between 8 and 120 characters")
        String newPassword,

        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}
