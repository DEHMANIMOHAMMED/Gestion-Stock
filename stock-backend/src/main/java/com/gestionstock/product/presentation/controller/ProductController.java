package com.gestionstock.product.presentation.controller;

import com.gestionstock.product.application.dto.*;
import com.gestionstock.product.application.mapper.ProductMapper;
import com.gestionstock.product.domain.service.ProductImportService;
import com.gestionstock.product.domain.service.ProductService;
import com.gestionstock.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;
    private final ProductImportService importService;
    private final ProductMapper mapper;
    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        permissionService.requireAdmin();
        var domain = mapper.toDomain(request);
        var saved = service.create(domain);
        return ResponseEntity.ok(mapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        var products = service.findAll();
        return ResponseEntity.ok(products.stream().map(mapper::toResponse).toList());
    }

    @PatchMapping
    public ResponseEntity<ProductResponse> update(
            @Valid @RequestBody ProductUpdateRequest request) {

        permissionService.requireAdmin();
        var domain = mapper.toDomain(request);
        var updated = service.update(domain);

        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.requireAdmin();
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<ProductImportResponse> importProducts(@RequestParam("file") MultipartFile file) {
        permissionService.requireAdmin();
        return ResponseEntity.ok(importService.importProducts(file));
    }

}
