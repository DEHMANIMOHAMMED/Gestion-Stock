package com.gestionstock.procurement.domain.service;

import com.gestionstock.procurement.application.dto.Supplier360OrderResponse;
import com.gestionstock.procurement.application.dto.Supplier360ProductResponse;
import com.gestionstock.procurement.application.dto.Supplier360Response;
import com.gestionstock.procurement.domain.model.ProductSupplier;
import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderLine;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.repository.ProductSupplierRepository;
import com.gestionstock.procurement.domain.repository.PurchaseOrderRepository;
import com.gestionstock.procurement.domain.repository.SupplierRepository;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Supplier360Service {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ProductRepository productRepository;
    private final ProductSupplierService productSupplierService;

    public Supplier360Response getSupplier360(Long supplierId) {
        Long organisationId = TenantContext.requireOrganisationId();
        Supplier supplier = supplierRepository.findById(supplierId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        List<PurchaseOrder> orders = purchaseOrderRepository.findAll(organisationId)
                .stream()
                .filter(order -> order.supplierId().equals(supplierId))
                .toList();
        List<ProductSupplier> productSuppliers = productSupplierRepository.findAll(organisationId)
                .stream()
                .filter(row -> row.supplierId().equals(supplierId))
                .filter(ProductSupplier::active)
                .toList();
        Map<Long, Product> products = productRepository.findAll(organisationId)
                .stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        int orderedQuantity = quantity(orders, false);
        int receivedQuantity = quantity(orders, true);
        double onTimeRate = onTimeRate(orders);
        double conformityRate = conformityRate(orderedQuantity, receivedQuantity);
        double averageDelayDays = averageDelayDays(orders);
        BigDecimal totalSpend = totalSpend(orders);
        BigDecimal averageUnitCost = averageUnitCost(orders);
        double healthScore = healthScore(onTimeRate, conformityRate, averageDelayDays);

        return new Supplier360Response(
                supplier.id(),
                supplier.name(),
                supplier.email(),
                supplier.phone(),
                supplier.leadTimeDays(),
                supplier.active(),
                orders.size(),
                countByStatus(orders, PurchaseOrderStatus.DRAFT),
                countByStatus(orders, PurchaseOrderStatus.ORDERED),
                countByStatus(orders, PurchaseOrderStatus.RECEIVED),
                lateOrders(orders),
                productSuppliers.size(),
                orderedQuantity,
                receivedQuantity,
                totalSpend,
                averageUnitCost,
                round(onTimeRate * 100),
                round(conformityRate * 100),
                round(averageDelayDays),
                round(healthScore),
                productResponses(productSuppliers, products),
                recentOrders(orders),
                recommendations(orders, productSuppliers, onTimeRate, conformityRate, averageDelayDays)
        );
    }

    private List<Supplier360ProductResponse> productResponses(List<ProductSupplier> productSuppliers, Map<Long, Product> products) {
        return productSuppliers.stream()
                .map(row -> {
                    Product product = products.get(row.productId());
                    ProductSupplierService.SupplierScore score = productSupplierService.score(row);
                    return new Supplier360ProductResponse(
                            row.productId(),
                            product == null ? "Product deleted" : product.name(),
                            product == null ? "-" : product.sku(),
                            row.unitCost(),
                            row.minimumOrderQuantity(),
                            row.preferred(),
                            score.score(),
                            score.explanation()
                    );
                })
                .toList();
    }

    private List<Supplier360OrderResponse> recentOrders(List<PurchaseOrder> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(PurchaseOrder::createdAt).reversed())
                .limit(8)
                .map(order -> new Supplier360OrderResponse(
                        order.id(),
                        order.status(),
                        order.expectedDeliveryDate(),
                        order.createdAt(),
                        order.receivedAt(),
                        order.lines().stream().mapToInt(line -> safe(line.quantity())).sum(),
                        order.lines().stream().mapToInt(line -> safe(line.receivedQuantity())).sum(),
                        orderCost(order)
                ))
                .toList();
    }

    private List<String> recommendations(
            List<PurchaseOrder> orders,
            List<ProductSupplier> productSuppliers,
            double onTimeRate,
            double conformityRate,
            double averageDelayDays
    ) {
        java.util.ArrayList<String> recommendations = new java.util.ArrayList<>();
        if (productSuppliers.isEmpty()) {
            recommendations.add("Associer ce fournisseur a des produits pour permettre les recommandations IA automatiques.");
        }
        if (orders.isEmpty()) {
            recommendations.add("Passer une premiere commande pilote pour construire un score fournisseur fiable.");
        }
        if (onTimeRate < 0.8) {
            recommendations.add("Renegocier le lead time ou prevoir un stock de securite plus haut: les livraisons sont trop souvent en retard.");
        }
        if (conformityRate < 0.9) {
            recommendations.add("Verifier les quantites livrees: le taux de reception conforme est sous 90%.");
        }
        if (averageDelayDays > 2) {
            recommendations.add("Ajouter une marge de delai dans les commandes IA pour absorber les retards moyens.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Fournisseur sain: conserver dans les recommandations IA et surveiller les couts.");
        }
        return recommendations;
    }

    private int countByStatus(List<PurchaseOrder> orders, PurchaseOrderStatus status) {
        return (int) orders.stream().filter(order -> order.status() == status).count();
    }

    private int quantity(List<PurchaseOrder> orders, boolean received) {
        return orders.stream()
                .flatMap(order -> order.lines().stream())
                .mapToInt(line -> safe(received ? line.receivedQuantity() : line.quantity()))
                .sum();
    }

    private int lateOrders(List<PurchaseOrder> orders) {
        return (int) orders.stream()
                .filter(order -> order.status() == PurchaseOrderStatus.RECEIVED)
                .filter(order -> order.expectedDeliveryDate() != null)
                .filter(order -> order.receivedAt() != null)
                .filter(order -> order.receivedAt().toLocalDate().isAfter(order.expectedDeliveryDate()))
                .count();
    }

    private double onTimeRate(List<PurchaseOrder> orders) {
        List<PurchaseOrder> receivedWithDate = orders.stream()
                .filter(order -> order.status() == PurchaseOrderStatus.RECEIVED)
                .filter(order -> order.expectedDeliveryDate() != null)
                .filter(order -> order.receivedAt() != null)
                .toList();
        if (receivedWithDate.isEmpty()) {
            return 1.0;
        }
        long onTime = receivedWithDate.stream()
                .filter(order -> !order.receivedAt().toLocalDate().isAfter(order.expectedDeliveryDate()))
                .count();
        return (double) onTime / receivedWithDate.size();
    }

    private double conformityRate(int orderedQuantity, int receivedQuantity) {
        if (orderedQuantity == 0) {
            return 1.0;
        }
        return Math.min(1.0, (double) receivedQuantity / orderedQuantity);
    }

    private double averageDelayDays(List<PurchaseOrder> orders) {
        List<Long> delays = orders.stream()
                .filter(order -> order.status() == PurchaseOrderStatus.RECEIVED)
                .filter(order -> order.expectedDeliveryDate() != null)
                .filter(order -> order.receivedAt() != null)
                .map(order -> Math.max(0, ChronoUnit.DAYS.between(order.expectedDeliveryDate(), order.receivedAt().toLocalDate())))
                .toList();
        if (delays.isEmpty()) {
            return 0.0;
        }
        return delays.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private BigDecimal totalSpend(List<PurchaseOrder> orders) {
        return orders.stream()
                .map(this::orderCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageUnitCost(List<PurchaseOrder> orders) {
        List<PurchaseOrderLine> pricedLines = orders.stream()
                .flatMap(order -> order.lines().stream())
                .filter(line -> line.unitCost() != null)
                .toList();
        int quantity = pricedLines.stream().mapToInt(line -> safe(line.quantity())).sum();
        if (quantity == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal cost = pricedLines.stream()
                .map(line -> line.unitCost().multiply(BigDecimal.valueOf(safe(line.quantity()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal orderCost(PurchaseOrder order) {
        return order.lines().stream()
                .filter(line -> line.unitCost() != null)
                .map(line -> line.unitCost().multiply(BigDecimal.valueOf(safe(line.quantity()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private double healthScore(double onTimeRate, double conformityRate, double averageDelayDays) {
        return Math.max(0.0, (onTimeRate * 45.0) + (conformityRate * 45.0) + Math.max(0.0, 10.0 - averageDelayDays * 2.0));
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
