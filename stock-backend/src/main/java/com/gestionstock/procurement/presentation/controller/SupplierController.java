package com.gestionstock.procurement.presentation.controller;

import com.gestionstock.procurement.application.dto.SupplierRequest;
import com.gestionstock.procurement.application.dto.SupplierResponse;
import com.gestionstock.procurement.application.dto.Supplier360Response;
import com.gestionstock.procurement.application.dto.SupplierComparisonResponse;
import com.gestionstock.procurement.application.dto.ProcurementImportResponse;
import com.gestionstock.procurement.application.dto.SupplierSlaRequest;
import com.gestionstock.procurement.application.dto.SupplierSlaResponse;
import com.gestionstock.procurement.application.mapper.SupplierMapper;
import com.gestionstock.procurement.domain.service.ProcurementImportService;
import com.gestionstock.procurement.domain.service.Supplier360Service;
import com.gestionstock.procurement.domain.service.SupplierIntelligenceService;
import com.gestionstock.procurement.domain.service.SupplierService;
import com.gestionstock.security.PlanAccessService;
import com.gestionstock.security.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;
    private final Supplier360Service supplier360Service;
    private final SupplierIntelligenceService supplierIntelligenceService;
    private final ProcurementImportService procurementImportService;
    private final SupplierMapper mapper;
    private final PermissionService permissionService;
    private final PlanAccessService planAccessService;

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        planAccessService.requireProPlan();
        permissionService.requireAdmin();
        return ResponseEntity.ok(mapper.toResponse(service.create(mapper.toDomain(request))));
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> findAll() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @PostMapping("/import")
    public ResponseEntity<ProcurementImportResponse> importSuppliers(@RequestParam("file") MultipartFile file) {
        planAccessService.requireProPlan();
        permissionService.requireAdmin();
        return ResponseEntity.ok(procurementImportService.importSuppliers(file));
    }

    @GetMapping("/{id}/360")
    public ResponseEntity<Supplier360Response> supplier360(@PathVariable Long id) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(supplier360Service.getSupplier360(id));
    }

    @GetMapping("/comparison")
    public ResponseEntity<List<SupplierComparisonResponse>> compare(@RequestParam(required = false) Long productId) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(supplierIntelligenceService.compareSuppliers(productId));
    }

    @GetMapping("/sla")
    public ResponseEntity<List<SupplierSlaResponse>> slas() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(supplierIntelligenceService.findSlas());
    }

    @PostMapping("/sla")
    public ResponseEntity<SupplierSlaResponse> upsertSla(@Valid @RequestBody SupplierSlaRequest request) {
        planAccessService.requireProPlan();
        permissionService.requireAdmin();
        return ResponseEntity.ok(supplierIntelligenceService.upsertSla(request));
    }
}
