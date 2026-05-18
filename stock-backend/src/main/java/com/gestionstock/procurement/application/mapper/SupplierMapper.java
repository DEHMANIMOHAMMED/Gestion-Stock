package com.gestionstock.procurement.application.mapper;

import com.gestionstock.procurement.application.dto.SupplierRequest;
import com.gestionstock.procurement.application.dto.SupplierResponse;
import com.gestionstock.procurement.domain.model.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisationId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Supplier toDomain(SupplierRequest request);

    SupplierResponse toResponse(Supplier supplier);
}
