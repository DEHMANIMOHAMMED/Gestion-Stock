package com.gestionstock.dashboard.application.service;

import com.gestionstock.dashboard.application.dto.DashboardSummaryResponse;
import com.gestionstock.dashboard.application.dto.LowStockAlertResponse;
import com.gestionstock.dashboard.application.dto.RecentStockMovementResponse;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.model.StockMovement;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_MOVEMENT_LIMIT = 8;

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    public DashboardSummaryResponse getSummary() {
        Long organisationId = TenantContext.requireOrganisationId();
        List<Product> products = productRepository.findAll(organisationId);
        Map<Long, Stock> stockByProductId = stockByProductId(organisationId);
        List<LowStockAlertResponse> alerts = findLowStockAlerts(products, stockByProductId);

        long totalUnits = stockByProductId.values()
                .stream()
                .mapToLong(Stock::quantity)
                .sum();

        long outOfStockProducts = products.stream()
                .filter(product -> quantityFor(product, stockByProductId) == 0)
                .count();

        List<RecentStockMovementResponse> recentMovements = recentMovements(organisationId, products);

        return new DashboardSummaryResponse(
                products.size(),
                totalUnits,
                alerts.size(),
                outOfStockProducts,
                recentMovements
        );
    }

    public List<LowStockAlertResponse> getLowStockAlerts() {
        Long organisationId = TenantContext.requireOrganisationId();
        return findLowStockAlerts(
                productRepository.findAll(organisationId),
                stockByProductId(organisationId)
        );
    }

    private Map<Long, Stock> stockByProductId(Long organisationId) {
        return stockRepository.findAll(organisationId)
                .stream()
                .collect(Collectors.toMap(Stock::productId, Function.identity()));
    }

    private List<LowStockAlertResponse> findLowStockAlerts(List<Product> products, Map<Long, Stock> stockByProductId) {
        return products.stream()
                .map(product -> toLowStockAlert(product, quantityFor(product, stockByProductId)))
                .filter(alert -> alert.quantity() <= alert.minStock())
                .sorted(Comparator.comparing(LowStockAlertResponse::missingQuantity).reversed())
                .toList();
    }

    private LowStockAlertResponse toLowStockAlert(Product product, int quantity) {
        int minStock = product.minStock() == null ? 0 : product.minStock();
        return new LowStockAlertResponse(
                product.id(),
                product.sku(),
                product.name(),
                quantity,
                minStock,
                Math.max(minStock - quantity, 0)
        );
    }

    private int quantityFor(Product product, Map<Long, Stock> stockByProductId) {
        Stock stock = stockByProductId.get(product.id());
        return stock == null ? 0 : stock.quantity();
    }

    private List<RecentStockMovementResponse> recentMovements(Long organisationId, List<Product> products) {
        Map<Long, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        return movementRepository.findRecent(organisationId, RECENT_MOVEMENT_LIMIT)
                .stream()
                .map(movement -> toRecentMovement(movement, productById.get(movement.productId())))
                .toList();
    }

    private RecentStockMovementResponse toRecentMovement(StockMovement movement, Product product) {
        return new RecentStockMovementResponse(
                movement.id(),
                movement.productId(),
                product == null ? "Produit supprime" : product.name(),
                movement.quantity(),
                movement.type().name(),
                movement.createdAt()
        );
    }
}
