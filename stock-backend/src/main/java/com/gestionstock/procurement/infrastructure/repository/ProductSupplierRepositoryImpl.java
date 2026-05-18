package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.domain.model.ProductSupplier;
import com.gestionstock.procurement.domain.repository.ProductSupplierRepository;
import com.gestionstock.procurement.infrastructure.entity.ProductSupplierEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductSupplierRepositoryImpl implements ProductSupplierRepository {

    private final ProductSupplierJpaRepository jpaRepository;

    @Override
    public ProductSupplier save(ProductSupplier productSupplier) {
        return toDomain(jpaRepository.save(toEntity(productSupplier)));
    }

    @Override
    public List<ProductSupplier> findByProduct(Long productId, Long organisationId) {
        return jpaRepository.findByOrganisationIdAndProductIdAndActiveTrue(organisationId, productId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ProductSupplier> findAll(Long organisationId) {
        return jpaRepository.findByOrganisationIdOrderByProductIdAscSupplierIdAsc(organisationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ProductSupplier> findByProductAndSupplier(Long productId, Long supplierId, Long organisationId) {
        return jpaRepository.findByOrganisationIdAndProductIdAndSupplierId(organisationId, productId, supplierId)
                .map(this::toDomain);
    }

    private ProductSupplier toDomain(ProductSupplierEntity entity) {
        return ProductSupplier.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .productId(entity.getProductId())
                .supplierId(entity.getSupplierId())
                .unitCost(entity.getUnitCost())
                .minimumOrderQuantity(entity.getMinimumOrderQuantity())
                .preferred(entity.getPreferred())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ProductSupplierEntity toEntity(ProductSupplier domain) {
        return ProductSupplierEntity.builder()
                .id(domain.id())
                .organisationId(domain.organisationId())
                .productId(domain.productId())
                .supplierId(domain.supplierId())
                .unitCost(domain.unitCost())
                .minimumOrderQuantity(domain.minimumOrderQuantity())
                .preferred(domain.preferred())
                .active(domain.active())
                .createdAt(domain.createdAt())
                .build();
    }
}
