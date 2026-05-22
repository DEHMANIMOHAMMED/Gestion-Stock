package com.gestionstock.procurement.presentation.controller;

import com.gestionstock.procurement.application.dto.ProductSupplierRequest;
import com.gestionstock.procurement.application.dto.ProductSupplierResponse;
import com.gestionstock.procurement.domain.model.ProductSupplier;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.service.ProductSupplierService;
import com.gestionstock.procurement.domain.service.SupplierService;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.service.ProductService;
import com.gestionstock.security.PlanAccessService;
import com.gestionstock.security.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product-suppliers")
@RequiredArgsConstructor
public class ProductSupplierController {

    private final ProductSupplierService service;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final PermissionService permissionService;
    private final PlanAccessService planAccessService;

    @PostMapping
    public ResponseEntity<ProductSupplierResponse> upsert(@Valid @RequestBody ProductSupplierRequest request) {
        planAccessService.requireProPlan();
        permissionService.requireAdmin();
        return ResponseEntity.ok(toResponse(service.upsert(ProductSupplier.builder()
                .productId(request.productId())
                .supplierId(request.supplierId())
                .unitCost(request.unitCost())
                .minimumOrderQuantity(request.minimumOrderQuantity())
                .preferred(Boolean.TRUE.equals(request.preferred()))
                .build())));
    }

    @GetMapping
    public ResponseEntity<List<ProductSupplierResponse>> findAll(@RequestParam(required = false) Long productId) {
        planAccessService.requireProPlan();
        List<ProductSupplier> productSuppliers = productId == null ? service.findAll() : service.findByProduct(productId);
        return ResponseEntity.ok(productSuppliers.stream().map(this::toResponse).toList());
    }

    private ProductSupplierResponse toResponse(ProductSupplier productSupplier) {
        Map<Long, Product> products = productService.findAll()
                .stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        Map<Long, Supplier> suppliers = supplierService.findAll()
                .stream()
                .collect(Collectors.toMap(Supplier::id, Function.identity()));
        Product product = products.get(productSupplier.productId());
        Supplier supplier = suppliers.get(productSupplier.supplierId());
        ProductSupplierService.SupplierScore score = service.score(productSupplier);
        return new ProductSupplierResponse(
                productSupplier.id(),
                productSupplier.organisationId(),
                productSupplier.productId(),
                product == null ? "Product deleted" : product.name(),
                product == null ? "-" : product.sku(),
                productSupplier.supplierId(),
                supplier == null ? "Supplier deleted" : supplier.name(),
                supplier == null ? null : supplier.leadTimeDays(),
                productSupplier.unitCost(),
                productSupplier.minimumOrderQuantity(),
                productSupplier.preferred(),
                score.score(),
                score.onTimeRate(),
                score.conformityRate(),
                score.explanation(),
                productSupplier.active(),
                productSupplier.createdAt()
        );
    }
}
