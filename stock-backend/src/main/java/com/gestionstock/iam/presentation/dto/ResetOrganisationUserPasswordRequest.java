package com.gestionstock.iam.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetOrganisationUserPasswordRequest(
        @NotBlank @Size(min = 8, max = 120) String temporaryPassword
) {
}
