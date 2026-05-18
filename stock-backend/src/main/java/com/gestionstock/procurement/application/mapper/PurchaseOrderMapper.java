package com.gestionstock.procurement.application.mapper;

import com.gestionstock.procurement.application.dto.*;
import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderLine;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.product.domain.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisationId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    PurchaseOrder toDomain(PurchaseOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receivedQuantity", ignore = true)
    PurchaseOrderLine toDomain(PurchaseOrderLineRequest request);

    default PurchaseOrderResponse toResponse(
            PurchaseOrder order,
            Supplier supplier,
            Map<Long, Product> products
    ) {
        return new PurchaseOrderResponse(
                order.id(),
                order.organisationId(),
                order.supplierId(),
                supplier == null ? "Supplier deleted" : supplier.name(),
                order.status(),
                order.expectedDeliveryDate(),
                order.createdAt(),
                order.receivedAt(),
                order.lines().stream()
                        .map(line -> toLineResponse(line, products.get(line.productId())))
                        .toList()
        );
    }

    private PurchaseOrderLineResponse toLineResponse(PurchaseOrderLine line, Product product) {
        return new PurchaseOrderLineResponse(
                line.id(),
                line.productId(),
                product == null ? "Product deleted" : product.name(),
                product == null ? "-" : product.sku(),
                line.quantity(),
                line.receivedQuantity(),
                line.unitCost()
        );
    }
}
