package com.gestionstock.product.domain.service;

import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService productService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRejectsMissingTenant() {
        assertThatThrownBy(() -> productService.create(request("sku-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant");
    }

    @Test
    void createNormalizesSkuAndSetsCurrentTenant() {
        TenantContext.setOrganisationId(42L);
        when(repository.existsBySku("SKU-1", 42L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        productService.create(request(" sku-1 "));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().organisationId()).isEqualTo(42L);
        assertThat(captor.getValue().sku()).isEqualTo("SKU-1");
    }

    private Product request(String sku) {
        return Product.builder()
                .name(" Product ")
                .sku(sku)
                .category(" Category ")
                .minStock(0)
                .unit(" pcs ")
                .build();
    }
}
