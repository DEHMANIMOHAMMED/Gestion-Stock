package com.gestionstock.procurement.application.dto;

import java.util.List;

public record ProcurementImportResponse(
        int created,
        int skipped,
        List<String> errors
) {
}
