package com.gestionstock.product.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_sku_organisation", columnNames = {"sku", "organisation_id"})
})
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 120)
    private String category;

    @Column(name = "min_stock", nullable = false)
    private Integer minStock;

    @Column(length = 32)
    private String unit;
}
