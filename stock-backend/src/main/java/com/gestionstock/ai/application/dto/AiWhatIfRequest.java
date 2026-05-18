package com.gestionstock.ai.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AiWhatIfRequest(
        @NotNull Long productId,
        @NotNull @Min(0) Integer orderQuantity,
        @NotNull @Min(0) Integer leadTimeDays
) {
}
