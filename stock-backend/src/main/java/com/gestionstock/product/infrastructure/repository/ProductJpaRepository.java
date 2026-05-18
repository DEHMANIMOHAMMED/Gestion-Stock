package com.gestionstock.product.infrastructure.repository;

import com.gestionstock.product.infrastructure.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySkuAndOrganisationId(String sku, Long orgId);

    Optional<ProductEntity> findBySkuAndOrganisationId(String sku, Long orgId);

    List<ProductEntity> findByOrganisationId(Long orgId);

    Optional<ProductEntity> findByIdAndOrganisationId(Long id, Long organisationId);

    void deleteByIdAndOrganisationId(Long id, Long organisationId);

}
