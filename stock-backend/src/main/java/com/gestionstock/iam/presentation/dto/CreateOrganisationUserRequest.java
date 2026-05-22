package com.gestionstock.iam.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganisationUserRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotBlank @Pattern(regexp = "ADMIN|USER", message = "Role must be ADMIN or USER") String role
) {
}
