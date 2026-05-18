package com.gestionstock.stock.domain.service;

import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.application.dto.StockMovementHistoryPageResponse;
import com.gestionstock.stock.application.dto.StockMovementHistoryResponse;
import com.gestionstock.stock.application.mapper.StockMovementMapper;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.model.StockMovement;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockMovementMapper stockMovementMapper;
    private final ProductRepository productRepository;

    @Transactional
    public StockMovement registerMovement(Long productId, Integer quantity, String type) {
        Long organisationId = TenantContext.requireOrganisationId();
        productRepository.findById(productId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        MovementType movementType = MovementType.from(type);

        Stock currentStock = stockRepository.findByProductForUpdate(productId, organisationId)
                .orElseGet(() -> Stock.builder()
                        .id(null)
                        .organisationId(organisationId)
                        .productId(productId)
                        .quantity(0)
                        .build());

        int newQuantity = switch (movementType) {
            case IN -> currentStock.quantity() + quantity;
            case OUT -> currentStock.quantity() - quantity;
            case ADJUST -> quantity;
        };

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Insufficient stock for product " + productId);
        }

        Stock updatedStock = Stock.builder()
                .id(currentStock.id())
                .organisationId(organisationId)
                .productId(productId)
                .quantity(newQuantity)
                .build();

        stockRepository.save(updatedStock);

        StockMovement movement = StockMovement.builder()
                .id(null)
                .organisationId(organisationId)
                .productId(productId)
                .quantity(quantity)
                .type(movementType)
                .createdAt(LocalDateTime.now())
                .build();

        return movementRepository.save(movement);
    }

    public Stock getStockForProduct(Long productId) {
        Long organisationId = TenantContext.requireOrganisationId();
        productRepository.findById(productId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return stockRepository.findByProduct(productId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found for product " + productId));
    }

    public StockMovementHistoryPageResponse getHistory(Long productId, String type, int page, int size) {

        Long organisationId = TenantContext.requireOrganisationId();
        MovementType normalizedType = normalizeType(type);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        var movements = movementRepository.findHistory(organisationId, productId, normalizedType, safePage, safeSize);
        var count = movementRepository.countHistory(organisationId, productId, normalizedType);

        var items = movements.stream()
                .map(stockMovementMapper::toHistoryResponse)
                .toList();

        return new StockMovementHistoryPageResponse(
                items,
                count,
                safePage,
                safeSize
        );
    }

    public List<StockMovementHistoryResponse> getAllHistory(Long productId, String type) {

        Long organisationId = TenantContext.requireOrganisationId();
        MovementType normalizedType = normalizeType(type);

        int page = 0;
        int size = 500;

        List<StockMovementHistoryResponse> result = new ArrayList<>();

        while (true) {
            var movements = movementRepository.findHistory(organisationId, productId, normalizedType, page, size);
            if (movements.isEmpty()) {
                break;
            }
            result.addAll(
                    movements.stream()
                            .map(stockMovementMapper::toHistoryResponse)
                            .toList()
            );
            page++;
        }

        return result;
    }

    private MovementType normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return MovementType.from(type);
    }
}
