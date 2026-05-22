package com.gestionstock.sales.infrastructure.repository;

import com.gestionstock.sales.domain.model.Sale;
import com.gestionstock.sales.domain.model.SaleLine;
import com.gestionstock.sales.domain.model.SalesDemand;
import com.gestionstock.sales.domain.repository.SalesRepository;
import com.gestionstock.sales.infrastructure.entity.SaleEntity;
import com.gestionstock.sales.infrastructure.entity.SaleLineEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SalesRepositoryImpl implements SalesRepository {

    private final SaleJpaRepository saleJpaRepository;
    private final SaleLineJpaRepository lineJpaRepository;

    @Override
    public Sale save(Sale sale) {
        SaleEntity savedSale = saleJpaRepository.save(toEntity(sale));
        lineJpaRepository.findBySaleId(savedSale.getId()).forEach(lineJpaRepository::delete);
        List<SaleLine> savedLines = sale.lines().stream()
                .map(line -> lineJpaRepository.save(toEntity(line, savedSale.getId(), savedSale.getOrganisationId())))
                .map(this::toDomain)
                .toList();
        return toDomain(savedSale, savedLines);
    }

    @Override
    public Optional<Sale> findById(Long id, Long organisationId) {
        return saleJpaRepository.findByIdAndOrganisationId(id, organisationId)
                .map(sale -> toDomain(sale, lineJpaRepository.findBySaleId(sale.getId()).stream().map(this::toDomain).toList()));
    }

    @Override
    public List<Sale> findRecent(Long organisationId, int limit) {
        var page = PageRequest.of(0, Math.max(1, limit), Sort.by("soldAt").descending());
        List<SaleEntity> sales = saleJpaRepository.findByOrganisationId(organisationId, page).toList();
        return withLines(sales);
    }

    @Override
    public List<Sale> findBetween(Long organisationId, LocalDateTime from, LocalDateTime to) {
        return withLines(saleJpaRepository.findByOrganisationIdAndSoldAtBetweenOrderBySoldAtDesc(organisationId, from, to));
    }

    @Override
    public List<SalesDemand> findDemandSince(Long organisationId, LocalDateTime since) {
        return lineJpaRepository.findDemandSince(organisationId, since).stream()
                .map(item -> new SalesDemand(item.getProductId(), item.getQuantity(), item.getSoldAt()))
                .toList();
    }

    private List<Sale> withLines(List<SaleEntity> sales) {
        if (sales.isEmpty()) {
            return List.of();
        }
        List<Long> saleIds = sales.stream().map(SaleEntity::getId).toList();
        Map<Long, List<SaleLine>> linesBySale = lineJpaRepository.findBySaleIdIn(saleIds).stream()
                .map(this::toDomain)
                .collect(Collectors.groupingBy(SaleLine::saleId));
        return sales.stream()
                .map(sale -> toDomain(sale, linesBySale.getOrDefault(sale.getId(), List.of())))
                .toList();
    }

    private SaleEntity toEntity(Sale sale) {
        return SaleEntity.builder()
                .id(sale.id())
                .organisationId(sale.organisationId())
                .reference(sale.reference())
                .customerName(sale.customerName())
                .channel(sale.channel())
                .status(sale.status())
                .totalAmount(sale.totalAmount())
                .soldAt(sale.soldAt())
                .createdAt(sale.createdAt())
                .build();
    }

    private SaleLineEntity toEntity(SaleLine line, Long saleId, Long organisationId) {
        return SaleLineEntity.builder()
                .id(line.id())
                .saleId(saleId)
                .organisationId(organisationId)
                .productId(line.productId())
                .quantity(line.quantity())
                .unitPrice(line.unitPrice())
                .lineTotal(line.lineTotal())
                .build();
    }

    private Sale toDomain(SaleEntity entity, List<SaleLine> lines) {
        return Sale.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .reference(entity.getReference())
                .customerName(entity.getCustomerName())
                .channel(entity.getChannel())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .soldAt(entity.getSoldAt())
                .createdAt(entity.getCreatedAt())
                .lines(lines)
                .build();
    }

    private SaleLine toDomain(SaleLineEntity entity) {
        return SaleLine.builder()
                .id(entity.getId())
                .saleId(entity.getSaleId())
                .organisationId(entity.getOrganisationId())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .lineTotal(entity.getLineTotal())
                .build();
    }
}
