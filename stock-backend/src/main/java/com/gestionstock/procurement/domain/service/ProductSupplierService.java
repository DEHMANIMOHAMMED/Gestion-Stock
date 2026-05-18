package com.gestionstock.procurement.domain.service;

import com.gestionstock.procurement.domain.model.ProductSupplier;
import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.repository.ProductSupplierRepository;
import com.gestionstock.procurement.domain.repository.PurchaseOrderRepository;
import com.gestionstock.procurement.domain.repository.SupplierRepository;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductSupplierService {

    private final ProductSupplierRepository repository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public ProductSupplier upsert(ProductSupplier request) {
        Long organisationId = TenantContext.requireOrganisationId();
        productRepository.findById(request.productId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        supplierRepository.findById(request.supplierId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        if (Boolean.TRUE.equals(request.preferred())) {
            unsetPreferredSupplier(request.productId(), organisationId);
        }

        ProductSupplier existing = repository
                .findByProductAndSupplier(request.productId(), request.supplierId(), organisationId)
                .orElse(null);

        return repository.save(ProductSupplier.builder()
                .id(existing == null ? null : existing.id())
                .organisationId(organisationId)
                .productId(request.productId())
                .supplierId(request.supplierId())
                .unitCost(request.unitCost())
                .minimumOrderQuantity(request.minimumOrderQuantity())
                .preferred(Boolean.TRUE.equals(request.preferred()))
                .active(true)
                .createdAt(existing == null ? LocalDateTime.now() : existing.createdAt())
                .build());
    }

    public List<ProductSupplier> findAll() {
        return repository.findAll(TenantContext.requireOrganisationId());
    }

    public List<ProductSupplier> findByProduct(Long productId) {
        return repository.findByProduct(productId, TenantContext.requireOrganisationId());
    }

    public Optional<ProductSupplier> findBestForProduct(Long productId, Long organisationId) {
        return findBestScoreForProduct(productId, organisationId).map(SupplierScore::productSupplier);
    }

    public Optional<SupplierScore> findBestScoreForProduct(Long productId, Long organisationId) {
        List<ProductSupplier> candidates = repository.findByProduct(productId, organisationId)
                .stream()
                .filter(ProductSupplier::active)
                .toList();

        double minCost = candidates.stream()
                .map(ProductSupplier::unitCost)
                .filter(cost -> cost != null && cost.signum() > 0)
                .mapToDouble(BigDecimal::doubleValue)
                .min()
                .orElse(1.0);

        return candidates.stream()
                .map(candidate -> score(candidate, organisationId, minCost))
                .min(java.util.Comparator.comparingDouble(SupplierScore::score));
    }

    public SupplierScore score(ProductSupplier productSupplier) {
        Long organisationId = TenantContext.requireOrganisationId();
        double minCost = repository.findByProduct(productSupplier.productId(), organisationId)
                .stream()
                .map(ProductSupplier::unitCost)
                .filter(cost -> cost != null && cost.signum() > 0)
                .mapToDouble(BigDecimal::doubleValue)
                .min()
                .orElse(1.0);
        return score(productSupplier, organisationId, minCost);
    }

    public Supplier supplierFor(ProductSupplier productSupplier, Long organisationId) {
        return supplierRepository.findById(productSupplier.supplierId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private void unsetPreferredSupplier(Long productId, Long organisationId) {
        repository.findByProduct(productId, organisationId)
                .stream()
                .filter(ProductSupplier::preferred)
                .forEach(candidate -> repository.save(ProductSupplier.builder()
                        .id(candidate.id())
                        .organisationId(candidate.organisationId())
                        .productId(candidate.productId())
                        .supplierId(candidate.supplierId())
                        .unitCost(candidate.unitCost())
                        .minimumOrderQuantity(candidate.minimumOrderQuantity())
                        .preferred(false)
                        .active(candidate.active())
                        .createdAt(candidate.createdAt())
                        .build()));
    }

    private Integer supplierLeadTime(Long supplierId, Long organisationId) {
        return supplierRepository.findById(supplierId, organisationId)
                .map(Supplier::leadTimeDays)
                .orElse(Integer.MAX_VALUE);
    }

    private SupplierScore score(ProductSupplier productSupplier, Long organisationId, double minCost) {
        Supplier supplier = supplierFor(productSupplier, organisationId);
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll(organisationId)
                .stream()
                .filter(order -> order.supplierId().equals(productSupplier.supplierId()))
                .filter(order -> order.status() != PurchaseOrderStatus.DRAFT)
                .filter(order -> order.status() != PurchaseOrderStatus.CANCELLED)
                .toList();

        double onTimeRate = onTimeRate(orders);
        double conformityRate = conformityRate(orders);
        double costScore = costScore(productSupplier.unitCost(), minCost);
        double leadTimeScore = Math.min(30.0, supplier.leadTimeDays() * 1.5);
        double reliabilityPenalty = (1.0 - onTimeRate) * 25.0;
        double conformityPenalty = (1.0 - conformityRate) * 25.0;
        double preferredBonus = Boolean.TRUE.equals(productSupplier.preferred()) ? -3.0 : 0.0;
        double score = Math.max(0.0, costScore + leadTimeScore + reliabilityPenalty + conformityPenalty + preferredBonus);

        String explanation = "Score base sur cout %.1f, lead time %.1f, fiabilite %.0f%%, conformite %.0f%%%s"
                .formatted(costScore, leadTimeScore, onTimeRate * 100, conformityRate * 100,
                        Boolean.TRUE.equals(productSupplier.preferred()) ? ", bonus preference" : "");

        return new SupplierScore(
                productSupplier,
                supplier,
                round(score),
                round(onTimeRate * 100),
                round(conformityRate * 100),
                explanation
        );
    }

    private double costScore(BigDecimal unitCost, double minCost) {
        if (unitCost == null || unitCost.signum() <= 0) {
            return 35.0;
        }
        return Math.min(60.0, (unitCost.doubleValue() / minCost) * 25.0);
    }

    private double onTimeRate(List<PurchaseOrder> orders) {
        List<PurchaseOrder> receivedWithExpectedDate = orders.stream()
                .filter(order -> order.status() == PurchaseOrderStatus.RECEIVED)
                .filter(order -> order.expectedDeliveryDate() != null)
                .filter(order -> order.receivedAt() != null)
                .toList();
        if (receivedWithExpectedDate.isEmpty()) {
            return 1.0;
        }
        long onTime = receivedWithExpectedDate.stream()
                .filter(order -> !order.receivedAt().toLocalDate().isAfter(order.expectedDeliveryDate()))
                .count();
        return (double) onTime / receivedWithExpectedDate.size();
    }

    private double conformityRate(List<PurchaseOrder> orders) {
        int ordered = orders.stream()
                .flatMap(order -> order.lines().stream())
                .mapToInt(line -> line.quantity() == null ? 0 : line.quantity())
                .sum();
        if (ordered == 0) {
            return 1.0;
        }
        int received = orders.stream()
                .flatMap(order -> order.lines().stream())
                .mapToInt(line -> line.receivedQuantity() == null ? 0 : line.receivedQuantity())
                .sum();
        return Math.min(1.0, (double) received / ordered);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record SupplierScore(
            ProductSupplier productSupplier,
            Supplier supplier,
            Double score,
            Double onTimeRate,
            Double conformityRate,
            String explanation
    ) {
    }
}
