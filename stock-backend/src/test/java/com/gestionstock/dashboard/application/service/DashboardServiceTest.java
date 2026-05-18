package com.gestionstock.dashboard.application.service;

import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getSummaryComputesTenantStockMetrics() {
        TenantContext.setOrganisationId(7L);
        when(productRepository.findAll(7L)).thenReturn(List.of(
                product(1L, "SKU-1", "Keyboard", 5),
                product(2L, "SKU-2", "Mouse", 3),
                product(3L, "SKU-3", "Screen", 2)
        ));
        when(stockRepository.findAll(7L)).thenReturn(List.of(
                stock(1L, 10),
                stock(2L, 1)
        ));
        when(movementRepository.findRecent(7L, 8)).thenReturn(List.of());

        var summary = dashboardService.getSummary();

        assertThat(summary.totalProducts()).isEqualTo(3);
        assertThat(summary.totalUnits()).isEqualTo(11);
        assertThat(summary.lowStockProducts()).isEqualTo(2);
        assertThat(summary.outOfStockProducts()).isEqualTo(1);
    }

    @Test
    void getLowStockAlertsSortsByMissingQuantity() {
        TenantContext.setOrganisationId(7L);
        when(productRepository.findAll(7L)).thenReturn(List.of(
                product(1L, "SKU-1", "Keyboard", 5),
                product(2L, "SKU-2", "Mouse", 8)
        ));
        when(stockRepository.findAll(7L)).thenReturn(List.of(
                stock(1L, 4),
                stock(2L, 1)
        ));

        var alerts = dashboardService.getLowStockAlerts();

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).sku()).isEqualTo("SKU-2");
        assertThat(alerts.get(0).missingQuantity()).isEqualTo(7);
    }

    private Product product(Long id, String sku, String name, Integer minStock) {
        return Product.builder()
                .id(id)
                .organisationId(7L)
                .sku(sku)
                .name(name)
                .minStock(minStock)
                .unit("pcs")
                .build();
    }

    private Stock stock(Long productId, Integer quantity) {
        return Stock.builder()
                .id(productId)
                .organisationId(7L)
                .productId(productId)
                .quantity(quantity)
                .build();
    }
}
