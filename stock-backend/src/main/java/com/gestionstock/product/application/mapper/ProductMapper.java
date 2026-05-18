package com.gestionstock.product.application.mapper;

import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.application.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisationId", ignore = true)
    Product toDomain(ProductRequest request);

    ProductResponse toResponse(Product product);

    @Mapping(target = "organisationId", ignore = true)
    Product toDomain(ProductUpdateRequest request);

}
