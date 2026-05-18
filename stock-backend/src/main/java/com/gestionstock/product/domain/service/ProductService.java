package com.gestionstock.product.domain.service;

import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public Product create(Product productRequest) {

        Long orgId = TenantContext.requireOrganisationId();
        String sku = normalizeSku(productRequest.sku());

        if (repository.existsBySku(sku, orgId)) {
            throw new IllegalArgumentException("SKU already exists in your organisation");
        }

        Product product = Product.builder()
                .id(null)
                .organisationId(orgId)
                .name(productRequest.name().trim())
                .sku(sku)
                .category(trimToNull(productRequest.category()))
                .minStock(productRequest.minStock())
                .unit(trimToNull(productRequest.unit()))
                .build();

        return repository.save(product);
    }

    public List<Product> findAll() {
        Long orgId = TenantContext.requireOrganisationId();
        return repository.findAll(orgId);
    }

    public Product update(Product updated) {

        Long orgId = TenantContext.requireOrganisationId();
        String sku = normalizeSku(updated.sku());

        var existing = repository.findById(updated.id(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!existing.sku().equals(sku) &&
                repository.existsBySku(sku, orgId)) {
            throw new IllegalArgumentException("SKU already exists in your organisation");
        }

        Product product = Product.builder()
                .id(existing.id())
                .organisationId(orgId)
                .name(updated.name().trim())
                .sku(sku)
                .category(trimToNull(updated.category()))
                .minStock(updated.minStock())
                .unit(trimToNull(updated.unit()))
                .build();

        return repository.save(product);
    }

    public void delete(Long id) {
        Long orgId = TenantContext.requireOrganisationId();

        var existing = repository.findById(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        repository.delete(id, orgId);
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
