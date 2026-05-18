package com.gestionstock.procurement.presentation.controller;

import com.gestionstock.procurement.application.dto.ProcurementApprovalSettingsRequest;
import com.gestionstock.procurement.application.dto.ProcurementApprovalSettingsResponse;
import com.gestionstock.procurement.application.dto.PurchaseOrderRequest;
import com.gestionstock.procurement.application.dto.PurchaseOrderResponse;
import com.gestionstock.procurement.application.dto.ProcurementImportResponse;
import com.gestionstock.procurement.application.dto.PurchaseOrderAuditLogResponse;
import com.gestionstock.procurement.application.dto.PurchaseOrderApprovalItemResponse;
import com.gestionstock.procurement.application.dto.PurchaseOrderFromRecommendationRequest;
import com.gestionstock.procurement.application.dto.PurchaseOrderReceiveRequest;
import com.gestionstock.procurement.application.mapper.PurchaseOrderMapper;
import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderAuditLog;
import com.gestionstock.procurement.domain.model.PurchaseOrderApprovalItem;
import com.gestionstock.procurement.domain.model.ProcurementApprovalSettings;
import com.gestionstock.procurement.domain.service.ProcurementApprovalSettingsService;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.service.PurchaseOrderDocumentService;
import com.gestionstock.procurement.domain.service.PurchaseOrderService;
import com.gestionstock.procurement.domain.service.ProcurementImportService;
import com.gestionstock.procurement.domain.service.SupplierService;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;
    private final ProcurementImportService procurementImportService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final PurchaseOrderDocumentService documentService;
    private final ProcurementApprovalSettingsService approvalSettingsService;
    private final PurchaseOrderMapper mapper;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.ok(toResponse(service.create(mapper.toDomain(request))));
    }

    @PostMapping("/from-recommendation")
    public ResponseEntity<PurchaseOrderResponse> createFromRecommendation(
            @Valid @RequestBody PurchaseOrderFromRecommendationRequest request
    ) {
        return ResponseEntity.ok(toResponse(
                service.createDraftFromRecommendation(request.recommendationId(), request.supplierId())
        ));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponse>> findAll() {
        return ResponseEntity.ok(service.findAll().stream().map(this::toResponse).toList());
    }

    @PostMapping("/import")
    public ResponseEntity<ProcurementImportResponse> importPurchaseOrders(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(procurementImportService.importPurchaseOrders(file));
    }

    @GetMapping("/approval-center")
    public ResponseEntity<List<PurchaseOrderApprovalItemResponse>> approvalCenter() {
        return ResponseEntity.ok(service.approvalCenter().stream().map(this::toApprovalResponse).toList());
    }

    @GetMapping("/approval-settings")
    public ResponseEntity<ProcurementApprovalSettingsResponse> approvalSettings() {
        ProcurementApprovalSettings settings = approvalSettingsService.currentSettings();
        return ResponseEntity.ok(toSettingsResponse(settings));
    }

    @PutMapping("/approval-settings")
    public ResponseEntity<ProcurementApprovalSettingsResponse> updateApprovalSettings(
            @Valid @RequestBody ProcurementApprovalSettingsRequest request
    ) {
        return ResponseEntity.ok(toSettingsResponse(approvalSettingsService.updateThreshold(request.approvalThreshold())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request
    ) {
        return ResponseEntity.ok(toResponse(service.updateDraft(id, mapper.toDomain(request))));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PurchaseOrderResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(service.confirm(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrderResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(service.approve(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(service.cancel(id)));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderResponse> receive(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid PurchaseOrderReceiveRequest request
    ) {
        Map<Long, Integer> quantities = request == null || request.lines() == null
                ? null
                : request.lines().stream().collect(Collectors.toMap(
                        line -> line.lineId(),
                        line -> line.quantity()
                ));
        return ResponseEntity.ok(toResponse(service.receive(id, quantities)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase-order-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(documentService.purchaseOrderPdf(id));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<PurchaseOrderAuditLogResponse>> audit(@PathVariable Long id) {
        return ResponseEntity.ok(service.auditLogs(id).stream().map(this::toAuditResponse).toList());
    }

    @GetMapping("/accounting-export")
    public ResponseEntity<byte[]> accountingExport() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase-orders-accounting.csv")
                .contentType(new MediaType("text", "csv"))
                .body(documentService.accountingExportCsv());
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        Supplier supplier = supplierService.findById(order.supplierId());
        Map<Long, Product> products = productService.findAll()
                .stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        return mapper.toResponse(order, supplier, products);
    }

    private PurchaseOrderAuditLogResponse toAuditResponse(PurchaseOrderAuditLog auditLog) {
        return new PurchaseOrderAuditLogResponse(
                auditLog.id(),
                auditLog.purchaseOrderId(),
                auditLog.action(),
                auditLog.previousStatus(),
                auditLog.newStatus(),
                auditLog.actorUserId(),
                auditLog.actorEmail(),
                auditLog.actorRole(),
                auditLog.orderTotal(),
                auditLog.createdAt()
        );
    }

    private PurchaseOrderApprovalItemResponse toApprovalResponse(PurchaseOrderApprovalItem item) {
        return new PurchaseOrderApprovalItemResponse(
                item.id(),
                item.supplierId(),
                item.supplierName(),
                item.status(),
                item.orderTotal(),
                item.linesCount(),
                item.totalQuantity(),
                item.maxRiskScore(),
                item.maxRiskLevel(),
                item.earliestStockoutDate(),
                item.urgency(),
                item.riskReason(),
                item.expectedDeliveryDate(),
                item.createdAt()
        );
    }

    private ProcurementApprovalSettingsResponse toSettingsResponse(ProcurementApprovalSettings settings) {
        return new ProcurementApprovalSettingsResponse(
                settings.approvalThreshold(),
                settings.updatedAt(),
                settings.updatedByUserId(),
                approvalSettingsService.isDefaultValue(settings)
        );
    }
}
