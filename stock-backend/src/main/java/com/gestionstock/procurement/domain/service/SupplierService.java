package com.gestionstock.procurement.domain.service;

import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.repository.SupplierRepository;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;

    public Supplier create(Supplier request) {
        Long organisationId = TenantContext.requireOrganisationId();
        String name = request.name().trim();

        if (repository.existsByName(name, organisationId)) {
            throw new IllegalArgumentException("Supplier already exists in your organisation");
        }

        return repository.save(Supplier.builder()
                .organisationId(organisationId)
                .name(name)
                .email(trimToNull(request.email()))
                .phone(trimToNull(request.phone()))
                .leadTimeDays(request.leadTimeDays())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public List<Supplier> findAll() {
        return repository.findAll(TenantContext.requireOrganisationId());
    }

    public Supplier findById(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        return repository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
