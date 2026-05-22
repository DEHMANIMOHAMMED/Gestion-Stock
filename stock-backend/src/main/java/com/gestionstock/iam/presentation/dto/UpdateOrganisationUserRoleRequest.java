package com.gestionstock.iam.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOrganisationUserRoleRequest(
        @NotBlank @Pattern(regexp = "ADMIN|USER", message = "Role must be ADMIN or USER") String role
) {
}
