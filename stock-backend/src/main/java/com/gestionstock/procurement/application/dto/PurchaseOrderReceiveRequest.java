package com.gestionstock.procurement.application.dto;

import jakarta.validation.Valid;

import java.util.List;

public record PurchaseOrderReceiveRequest(
        @Valid List<PurchaseOrderReceiveLineRequest> lines
) {
}
