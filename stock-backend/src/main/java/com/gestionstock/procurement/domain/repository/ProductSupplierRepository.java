package com.gestionstock.procurement.domain.repository;

import com.gestionstock.procurement.domain.model.ProductSupplier;

import java.util.List;
import java.util.Optional;

public interface ProductSupplierRepository {
    ProductSupplier save(ProductSupplier productSupplier);

    List<ProductSupplier> findByProduct(Long productId, Long organisationId);

    List<ProductSupplier> findAll(Long organisationId);

    Optional<ProductSupplier> findByProductAndSupplier(Long productId, Long supplierId, Long organisationId);
}
