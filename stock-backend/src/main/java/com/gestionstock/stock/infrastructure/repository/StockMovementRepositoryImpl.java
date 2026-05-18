package com.gestionstock.stock.infrastructure.repository;

import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.StockMovement;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.infrastructure.entity.StockMovementEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockMovementRepositoryImpl implements StockMovementRepository {

    private final StockMovementJpaRepository jpaRepository;

    @Override
    public StockMovement save(StockMovement movement) {
        StockMovementEntity entity = toEntity(movement);
        StockMovementEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<StockMovement> findHistory(Long organisationId, Long productId, MovementType type, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        var result =
                (productId != null && type != null) ? jpaRepository.findByOrganisationIdAndProductIdAndType(organisationId, productId, type, pageable) :
                        (productId != null) ? jpaRepository.findByOrganisationIdAndProductId(organisationId, productId, pageable) :
                                (type != null) ? jpaRepository.findByOrganisationIdAndType(organisationId, type, pageable) :
                                        jpaRepository.findByOrganisationId(organisationId, pageable);

        return result.map(this::toDomain).toList();
    }

    @Override
    public long countHistory(Long organisationId, Long productId, MovementType type) {
        return (productId != null && type != null) ? jpaRepository.countByOrganisationIdAndProductIdAndType(organisationId, productId, type) :
                (productId != null) ? jpaRepository.countByOrganisationIdAndProductId(organisationId, productId) :
                        (type != null) ? jpaRepository.countByOrganisationIdAndType(organisationId, type) :
                                jpaRepository.countByOrganisationId(organisationId);
    }

    @Override
    public List<StockMovement> findRecent(Long organisationId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1), Sort.by("createdAt").descending());
        return jpaRepository.findByOrganisationId(organisationId, pageable)
                .map(this::toDomain)
                .toList();
    }

    private StockMovementEntity toEntity(StockMovement domain) {
        return StockMovementEntity.builder()
                .id(domain.id())
                .organisationId(domain.organisationId())
                .productId(domain.productId())
                .quantity(domain.quantity())
                .type(domain.type())
                .createdAt(domain.createdAt())
                .build();
    }

    private StockMovement toDomain(StockMovementEntity entity) {
        return StockMovement.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
