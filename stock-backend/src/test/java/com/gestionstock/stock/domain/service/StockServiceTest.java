package com.gestionstock.stock.domain.service;

import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.application.mapper.StockMovementMapper;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.model.StockMovement;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private StockMovementMapper stockMovementMapper;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockService stockService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registerMovementRejectsMissingTenant() {
        assertThatThrownBy(() -> stockService.registerMovement(10L, 1, "IN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant");
    }

    @Test
    void registerMovementRejectsProductOutsideCurrentTenant() {
        TenantContext.setOrganisationId(1L);
        when(productRepository.findById(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.registerMovement(10L, 1, "IN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product not found");

        verify(stockRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void registerMovementRejectsOutWhenStockIsInsufficient() {
        TenantContext.setOrganisationId(1L);
        when(productRepository.findById(10L, 1L)).thenReturn(Optional.of(product()));
        when(stockRepository.findByProductForUpdate(10L, 1L)).thenReturn(Optional.of(
                Stock.builder()
                        .id(99L)
                        .organisationId(1L)
                        .productId(10L)
                        .quantity(3)
                        .build()
        ));

        assertThatThrownBy(() -> stockService.registerMovement(10L, 4, "OUT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        verify(stockRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void registerMovementPersistsStockAndHistoryForValidOut() {
        TenantContext.setOrganisationId(1L);
        when(productRepository.findById(10L, 1L)).thenReturn(Optional.of(product()));
        when(stockRepository.findByProductForUpdate(10L, 1L)).thenReturn(Optional.of(
                Stock.builder()
                        .id(99L)
                        .organisationId(1L)
                        .productId(10L)
                        .quantity(8)
                        .build()
        ));
        when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement movement = stockService.registerMovement(10L, 3, "out");

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().quantity()).isEqualTo(5);
        assertThat(movement.type()).isEqualTo(MovementType.OUT);
    }

    private Product product() {
        return Product.builder()
                .id(10L)
                .organisationId(1L)
                .name("Keyboard")
                .sku("KEYBOARD")
                .minStock(1)
                .unit("pcs")
                .build();
    }
}
