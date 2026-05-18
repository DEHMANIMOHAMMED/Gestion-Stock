package com.gestionstock.procurement.domain.service;

import com.gestionstock.procurement.application.dto.SupplierComparisonResponse;
import com.gestionstock.procurement.application.dto.SupplierSlaRequest;
import com.gestionstock.procurement.application.dto.SupplierSlaResponse;
import com.gestionstock.procurement.domain.model.ProductSupplier;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.infrastructure.entity.SupplierSlaSettingsEntity;
import com.gestionstock.procurement.infrastructure.repository.SupplierSlaSettingsJpaRepository;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierIntelligenceService {

    private final ProductSupplierService productSupplierService;
    private final SupplierService supplierService;
    private final ProductRepository productRepository;
    private final SupplierSlaSettingsJpaRepository slaRepository;

    public List<SupplierComparisonResponse> compareSuppliers(Long productId) {
        Long organisationId = TenantContext.requireOrganisationId();
        Map<Long, Product> products = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        Map<Long, Supplier> suppliers = supplierService.findAll().stream()
                .collect(Collectors.toMap(Supplier::id, Function.identity()));
        Map<Long, Long> bestSupplierByProduct = productSupplierService.findAll().stream()
                .collect(Collectors.groupingBy(
                        ProductSupplier::productId,
                        Collectors.collectingAndThen(Collectors.toList(), candidates -> candidates.stream()
                                .map(productSupplierService::score)
                                .min(Comparator.comparing(ProductSupplierService.SupplierScore::score))
                                .map(score -> score.productSupplier().supplierId())
                                .orElse(null)
                        )
                ));

        return productSupplierService.findAll().stream()
                .filter(item -> productId == null || item.productId().equals(productId))
                .map(item -> {
                    Product product = products.get(item.productId());
                    Supplier supplier = suppliers.get(item.supplierId());
                    ProductSupplierService.SupplierScore score = productSupplierService.score(item);
                    boolean best = item.supplierId().equals(bestSupplierByProduct.get(item.productId()));
                    return new SupplierComparisonResponse(
                            item.productId(),
                            product == null ? "Product deleted" : product.name(),
                            product == null ? "-" : product.sku(),
                            item.supplierId(),
                            supplier == null ? "Supplier deleted" : supplier.name(),
                            item.unitCost(),
                            item.minimumOrderQuantity(),
                            supplier == null ? null : supplier.leadTimeDays(),
                            score.score(),
                            score.onTimeRate(),
                            score.conformityRate(),
                            item.preferred(),
                            best,
                            best ? "Meilleur fournisseur actuel selon cout, lead time et fiabilite."
                                    : "Alternative possible, mais moins performante que le meilleur score."
                    );
                })
                .sorted(Comparator.comparing(SupplierComparisonResponse::productName)
                        .thenComparing(SupplierComparisonResponse::supplierScore))
                .toList();
    }

    public SupplierSlaResponse upsertSla(SupplierSlaRequest request) {
        Long organisationId = TenantContext.requireOrganisationId();
        Supplier supplier = supplierService.findById(request.supplierId());
        SupplierSlaSettingsEntity existing = slaRepository.findByOrganisationIdAndSupplierId(organisationId, request.supplierId())
                .orElse(null);
        SupplierSlaSettingsEntity saved = slaRepository.save(SupplierSlaSettingsEntity.builder()
                .id(existing == null ? null : existing.getId())
                .organisationId(organisationId)
                .supplierId(request.supplierId())
                .targetLeadTimeDays(request.targetLeadTimeDays())
                .targetOnTimeRate(request.targetOnTimeRate())
                .targetConformityRate(request.targetConformityRate())
                .notes(request.notes())
                .updatedAt(LocalDateTime.now())
                .build());
        return toResponse(saved, supplier);
    }

    public List<SupplierSlaResponse> findSlas() {
        Long organisationId = TenantContext.requireOrganisationId();
        Map<Long, Supplier> suppliers = supplierService.findAll().stream()
                .collect(Collectors.toMap(Supplier::id, Function.identity()));
        return slaRepository.findByOrganisationId(organisationId).stream()
                .map(sla -> toResponse(sla, suppliers.get(sla.getSupplierId())))
                .toList();
    }

    private SupplierSlaResponse toResponse(SupplierSlaSettingsEntity entity, Supplier supplier) {
        return new SupplierSlaResponse(
                entity.getSupplierId(),
                supplier == null ? "Supplier deleted" : supplier.name(),
                entity.getTargetLeadTimeDays(),
                entity.getTargetOnTimeRate(),
                entity.getTargetConformityRate(),
                entity.getNotes(),
                entity.getUpdatedAt()
        );
    }
}
