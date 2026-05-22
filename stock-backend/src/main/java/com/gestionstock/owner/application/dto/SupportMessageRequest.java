package com.gestionstock.owner.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportMessageRequest(
        @NotBlank(message = "Subject is required")
        @Size(max = 160, message = "Subject must be at most 160 characters")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message must be at most 4000 characters")
        String message,

        @Size(max = 220, message = "Attachment name must be at most 220 characters")
        String attachmentName,

        @Size(max = 120, message = "Attachment content type must be at most 120 characters")
        String attachmentContentType,

        @Size(max = 3_000_000, message = "Attachment is too large")
        String attachmentData
) {
}
