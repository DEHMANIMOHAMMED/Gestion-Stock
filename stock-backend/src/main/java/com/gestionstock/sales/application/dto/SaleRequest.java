package com.gestionstock.sales.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaleRequest(
        String customerName,
        @NotBlank String channel,
        @NotEmpty List<@Valid SaleLineRequest> lines
) {}
