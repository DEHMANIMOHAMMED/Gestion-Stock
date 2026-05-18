package com.gestionstock.product.infrastructure.repository;

import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpa;

    private Product toDomain(ProductEntity e) {
        return Product.builder()
                .id(e.getId())
                .organisationId(e.getOrganisationId())
                .name(e.getName())
                .sku(e.getSku())
                .category(e.getCategory())
                .minStock(e.getMinStock())
                .unit(e.getUnit())
                .build();
    }

    private ProductEntity toEntity(Product d) {
        return ProductEntity.builder()
                .id(d.id())
                .organisationId(d.organisationId())
                .name(d.name())
                .sku(d.sku())
                .category(d.category())
                .minStock(d.minStock())
                .unit(d.unit())
                .build();
    }

    @Override
    public Product save(Product product) {
        return toDomain(jpa.save(toEntity(product)));
    }

    @Override
    public boolean existsBySku(String sku, Long orgId) {
        return jpa.existsBySkuAndOrganisationId(sku, orgId);
    }

    @Override
    public Optional<Product> findBySku(String sku, Long orgId) {
        return jpa.findBySkuAndOrganisationId(sku, orgId).map(this::toDomain);
    }

    @Override
    public List<Product> findAll(Long orgId) {
        return jpa.findByOrganisationId(orgId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Product> findById(Long id, Long orgId) {
        return jpa.findByIdAndOrganisationId(id, orgId).map(this::toDomain);
    }

    @Override
    public void delete(Long id, Long orgId) {
        jpa.deleteByIdAndOrganisationId(id, orgId);
    }


}
