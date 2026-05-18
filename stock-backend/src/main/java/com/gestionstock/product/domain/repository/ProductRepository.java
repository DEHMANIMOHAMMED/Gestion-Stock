package com.gestionstock.product.domain.repository;

import com.gestionstock.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    List<Product> findAll(Long orgId);

    Optional<Product> findBySku(String sku, Long orgId);

    boolean existsBySku(String sku, Long orgId);
    Optional<Product> findById(Long id, Long orgId);

    void delete(Long id, Long orgId);

}
