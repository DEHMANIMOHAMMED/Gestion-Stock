package com.gestionstock.procurement.infrastructure.repository;

import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.repository.SupplierRepository;
import com.gestionstock.procurement.infrastructure.entity.SupplierEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SupplierRepositoryImpl implements SupplierRepository {

    private final SupplierJpaRepository jpaRepository;

    @Override
    public Supplier save(Supplier supplier) {
        return toDomain(jpaRepository.save(toEntity(supplier)));
    }

    @Override
    public List<Supplier> findAll(Long organisationId) {
        return jpaRepository.findByOrganisationIdOrderByNameAsc(organisationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Supplier> findById(Long id, Long organisationId) {
        return jpaRepository.findByIdAndOrganisationId(id, organisationId).map(this::toDomain);
    }

    @Override
    public boolean existsByName(String name, Long organisationId) {
        return jpaRepository.existsByNameAndOrganisationId(name, organisationId);
    }

    private Supplier toDomain(SupplierEntity entity) {
        return Supplier.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .leadTimeDays(entity.getLeadTimeDays())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private SupplierEntity toEntity(Supplier supplier) {
        return SupplierEntity.builder()
                .id(supplier.id())
                .organisationId(supplier.organisationId())
                .name(supplier.name())
                .email(supplier.email())
                .phone(supplier.phone())
                .leadTimeDays(supplier.leadTimeDays())
                .active(supplier.active())
                .createdAt(supplier.createdAt())
                .build();
    }
}
