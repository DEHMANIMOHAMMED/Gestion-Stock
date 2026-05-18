package com.gestionstock.notification.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationActionRequest(
        @NotBlank String action,
        @Size(max = 600) String reason
) {
}
