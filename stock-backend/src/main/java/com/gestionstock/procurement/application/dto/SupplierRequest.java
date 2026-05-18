package com.gestionstock.procurement.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Supplier name is required")
        @Size(max = 160, message = "Supplier name must be at most 160 characters")
        String name,

        @Email(message = "Supplier email must be valid")
        @Size(max = 180, message = "Supplier email must be at most 180 characters")
        String email,

        @Size(max = 60, message = "Supplier phone must be at most 60 characters")
        String phone,

        @NotNull(message = "Lead time is required")
        @Min(value = 0, message = "Lead time must be zero or greater")
        Integer leadTimeDays
) {
}
