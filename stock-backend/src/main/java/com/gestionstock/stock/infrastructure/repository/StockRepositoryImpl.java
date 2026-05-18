package com.gestionstock.stock.infrastructure.repository;

import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.repository.StockRepository;
import com.gestionstock.stock.infrastructure.entity.StockEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepository {

    private final StockJpaRepository jpaRepository;

    @Override
    public Optional<Stock> findByProduct(Long productId, Long organisationId) {
        return jpaRepository.findByProductIdAndOrganisationId(productId, organisationId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Stock> findByProductForUpdate(Long productId, Long organisationId) {
        return jpaRepository.findByProductIdAndOrganisationIdForUpdate(productId, organisationId)
                .map(this::toDomain);
    }

    @Override
    public List<Stock> findAll(Long organisationId) {
        return jpaRepository.findByOrganisationId(organisationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Stock save(Stock stock) {
        StockEntity entity = toEntity(stock);
        StockEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private Stock toDomain(StockEntity entity) {
        return Stock.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .build();
    }

    private StockEntity toEntity(Stock domain) {
        return StockEntity.builder()
                .id(domain.id())
                .organisationId(domain.organisationId())
                .productId(domain.productId())
                .quantity(domain.quantity())
                .build();
    }
}
