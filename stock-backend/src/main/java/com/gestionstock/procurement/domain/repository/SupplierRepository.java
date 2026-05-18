package com.gestionstock.procurement.domain.repository;

import com.gestionstock.procurement.domain.model.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository {
    Supplier save(Supplier supplier);

    List<Supplier> findAll(Long organisationId);

    Optional<Supplier> findById(Long id, Long organisationId);

    boolean existsByName(String name, Long organisationId);
}
