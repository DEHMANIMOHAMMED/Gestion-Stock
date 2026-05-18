package com.gestionstock.procurement.domain.service;

import com.gestionstock.audit.AuditLogService;
import com.gestionstock.ai.infrastructure.repository.AiReorderRecommendationRepository;
import com.gestionstock.ai.infrastructure.entity.AiStockoutRiskEntity;
import com.gestionstock.ai.infrastructure.repository.AiStockoutRiskRepository;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.notification.domain.service.AdminNotificationService;
import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderAuditLog;
import com.gestionstock.procurement.domain.model.PurchaseOrderApprovalItem;
import com.gestionstock.procurement.domain.model.PurchaseOrderLine;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.repository.PurchaseOrderAuditLogRepository;
import com.gestionstock.procurement.domain.repository.PurchaseOrderRepository;
import com.gestionstock.procurement.domain.repository.SupplierRepository;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.domain.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final AiReorderRecommendationRepository recommendationRepository;
    private final AiStockoutRiskRepository stockoutRiskRepository;
    private final ProductSupplierService productSupplierService;
    private final PurchaseOrderAuditLogRepository auditLogRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ProcurementApprovalSettingsService approvalSettingsService;
    private final AdminNotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public PurchaseOrder create(PurchaseOrder request) {
        Long organisationId = TenantContext.requireOrganisationId();

        supplierRepository.findById(request.supplierId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        List<PurchaseOrderLine> lines = request.lines().stream()
                .map(line -> validateLine(line, organisationId))
                .toList();

        PurchaseOrder order = PurchaseOrder.builder()
                .organisationId(organisationId)
                .supplierId(request.supplierId())
                .status(PurchaseOrderStatus.ORDERED)
                .expectedDeliveryDate(request.expectedDeliveryDate())
                .createdAt(LocalDateTime.now())
                .lines(lines)
                .build();

        requireCanPlaceOrder(order);
        PurchaseOrder savedOrder = orderRepository.save(order);
        auditStatusChange(savedOrder, null, savedOrder.status(), "CREATED_ORDERED");
        auditPurchaseOrder(savedOrder, "PURCHASE_ORDER_CREATED", "Commande fournisseur creee en statut " + savedOrder.status());
        notifyIfAboveApprovalThreshold(savedOrder);
        return savedOrder;
    }

    @Transactional
    public PurchaseOrder createDraftFromRecommendation(Long recommendationId, Long supplierId) {
        Long organisationId = TenantContext.requireOrganisationId();

        if (recommendationId == null) {
            throw new IllegalArgumentException("Recommendation not found");
        }

        var recommendation = recommendationRepository.findByIdAndOrganisationId(recommendationId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));

        productRepository.findById(recommendation.getProductId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + recommendation.getProductId()));

        ProductSupplierService.SupplierScore supplierScore = supplierId == null
                ? productSupplierService.findBestScoreForProduct(recommendation.getProductId(), organisationId)
                        .orElseThrow(() -> new IllegalArgumentException("No preferred supplier configured for this product"))
                : null;
        var productSupplier = supplierId == null
                ? supplierScore.productSupplier()
                : productSupplierService.findByProduct(recommendation.getProductId()).stream()
                        .filter(candidate -> candidate.supplierId().equals(supplierId))
                        .findFirst()
                        .orElse(null);

        supplierRepository.findById(supplierId == null ? productSupplier.supplierId() : supplierId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        Long selectedSupplierId = supplierId == null ? productSupplier.supplierId() : supplierId;
        Integer quantity = productSupplier == null
                ? recommendation.getRecommendedQuantity()
                : Math.max(recommendation.getRecommendedQuantity(), productSupplier.minimumOrderQuantity());

        PurchaseOrder order = PurchaseOrder.builder()
                .organisationId(organisationId)
                .supplierId(selectedSupplierId)
                .status(PurchaseOrderStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .lines(List.of(PurchaseOrderLine.builder()
                        .productId(recommendation.getProductId())
                        .quantity(quantity)
                        .receivedQuantity(0)
                        .unitCost(productSupplier == null ? null : productSupplier.unitCost())
                        .build()))
                .build();

        if (recommendation.getPurchaseOrderId() != null) {
            throw new IllegalArgumentException("Recommendation already linked to a purchase order");
        }

        PurchaseOrder savedOrder = orderRepository.save(order);
        auditStatusChange(savedOrder, null, savedOrder.status(), "CREATED_DRAFT_FROM_AI");
        auditPurchaseOrder(savedOrder, "PURCHASE_ORDER_CREATED_FROM_AI",
                "Commande brouillon creee depuis recommandation IA #" + recommendationId);
        notifyIfAboveApprovalThreshold(savedOrder);
        recommendation.setStatus("APPROVED");
        recommendation.setPurchaseOrderId(savedOrder.id());
        recommendationRepository.save(recommendation);

        return savedOrder;
    }

    public List<PurchaseOrder> findAll() {
        return orderRepository.findAll(TenantContext.requireOrganisationId());
    }

    public List<PurchaseOrderApprovalItem> approvalCenter() {
        requireAdmin("Only admins can access the approval center");
        Long organisationId = TenantContext.requireOrganisationId();
        Map<Long, Supplier> suppliers = supplierRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Supplier::id, Function.identity()));
        Map<Long, AiStockoutRiskEntity> risksByProduct = stockoutRiskRepository
                .findByOrganisationIdOrderByRiskScoreDesc(organisationId)
                .stream()
                .collect(Collectors.toMap(
                        AiStockoutRiskEntity::getProductId,
                        Function.identity(),
                        (first, second) -> first
                ));

        return orderRepository.findAll(organisationId).stream()
                .filter(order -> order.status() == PurchaseOrderStatus.DRAFT
                        || order.status() == PurchaseOrderStatus.APPROVED)
                .map(order -> {
                    PurchaseOrderApprovalItem item = toApprovalItem(order, suppliers.get(order.supplierId()), risksByProduct);
                    notifyIfCriticalPendingOrder(item);
                    return item;
                })
                .sorted(Comparator
                        .comparing((PurchaseOrderApprovalItem item) -> urgencyRank(item.urgency()))
                        .thenComparing(PurchaseOrderApprovalItem::orderTotal)
                        .reversed())
                .toList();
    }

    @Transactional
    public PurchaseOrder updateDraft(Long id, PurchaseOrder request) {
        Long organisationId = TenantContext.requireOrganisationId();
        PurchaseOrder order = orderRepository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (order.status() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchase orders can be updated");
        }

        supplierRepository.findById(request.supplierId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        List<PurchaseOrderLine> lines = request.lines().stream()
                .map(line -> validateLine(line, organisationId))
                .toList();

        PurchaseOrder updated = PurchaseOrder.builder()
                .id(order.id())
                .organisationId(order.organisationId())
                .supplierId(request.supplierId())
                .status(PurchaseOrderStatus.DRAFT)
                .expectedDeliveryDate(request.expectedDeliveryDate())
                .createdAt(order.createdAt())
                .receivedAt(null)
                .lines(lines)
                .build();

        PurchaseOrder savedOrder = orderRepository.save(updated);
        auditPurchaseOrder(savedOrder, "PURCHASE_ORDER_UPDATED_DRAFT",
                "Commande brouillon mise a jour, total " + orderTotal(savedOrder));
        notifyIfAboveApprovalThreshold(savedOrder);
        return savedOrder;
    }

    @Transactional
    public PurchaseOrder approve(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        PurchaseOrder order = orderRepository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (order.status() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchase orders can be approved");
        }

        requireAdmin("Only admins can approve purchase orders");
        PurchaseOrder savedOrder = orderRepository.save(copyWithStatus(order, PurchaseOrderStatus.APPROVED, null));
        auditStatusChange(savedOrder, order.status(), savedOrder.status(), "APPROVED");
        auditPurchaseOrder(savedOrder, "PURCHASE_ORDER_APPROVED", "Commande fournisseur approuvee");
        return savedOrder;
    }

    @Transactional
    public PurchaseOrder confirm(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        PurchaseOrder order = orderRepository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (order.status() != PurchaseOrderStatus.DRAFT && order.status() != PurchaseOrderStatus.APPROVED) {
            throw new IllegalArgumentException("Only draft or approved purchase orders can be confirmed");
        }

        requireCanConfirm(order);
        PurchaseOrder savedOrder = orderRepository.save(copyWithStatus(order, PurchaseOrderStatus.ORDERED, null));
        auditStatusChange(savedOrder, order.status(), savedOrder.status(), "CONFIRMED");
        auditPurchaseOrder(savedOrder, "PURCHASE_ORDER_CONFIRMED", "Commande fournisseur confirmee");
        return savedOrder;
    }

    @Transactional
    public PurchaseOrder cancel(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        PurchaseOrder order = orderRepository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (order.status() == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalArgumentException("Received purchase order cannot be cancelled");
        }
        if (order.status() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Purchase order already cancelled");
        }
        requireCanCancel(order);

        PurchaseOrder savedOrder = orderRepository.save(copyWithStatus(order, PurchaseOrderStatus.CANCELLED, null));
        auditStatusChange(savedOrder, order.status(), savedOrder.status(), "CANCELLED");
        auditPurchaseOrder(savedOrder, "PURCHASE_ORDER_CANCELLED", "Commande fournisseur annulee");
        return savedOrder;
    }

    @Transactional
    public PurchaseOrder receive(Long id, Map<Long, Integer> receivedQuantities) {
        Long organisationId = TenantContext.requireOrganisationId();
        PurchaseOrder order = orderRepository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (order.status() == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalArgumentException("Purchase order already received");
        }
        if (order.status() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled purchase order cannot be received");
        }
        if (order.status() != PurchaseOrderStatus.ORDERED) {
            throw new IllegalArgumentException("Only ordered purchase orders can be received");
        }

        Map<Long, PurchaseOrderLine> linesById = order.lines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::id, Function.identity()));
        Map<Long, Integer> quantities = receivedQuantities == null
                ? order.lines().stream().collect(Collectors.toMap(
                        PurchaseOrderLine::id,
                        line -> line.quantity() - line.receivedQuantity()
                ))
                : receivedQuantities;

        List<PurchaseOrderLine> updatedLines = order.lines().stream()
                .map(line -> receiveLine(line, quantities.get(line.id())))
                .toList();

        quantities.keySet().forEach(lineId -> {
            if (!linesById.containsKey(lineId)) {
                throw new IllegalArgumentException("Purchase order line not found: " + lineId);
            }
        });

        boolean fullyReceived = updatedLines.stream()
                .allMatch(line -> line.receivedQuantity().equals(line.quantity()));

        PurchaseOrder received = PurchaseOrder.builder()
                .id(order.id())
                .organisationId(order.organisationId())
                .supplierId(order.supplierId())
                .status(fullyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.ORDERED)
                .expectedDeliveryDate(order.expectedDeliveryDate())
                .createdAt(order.createdAt())
                .receivedAt(fullyReceived ? LocalDateTime.now() : null)
                .lines(updatedLines)
                .build();

        PurchaseOrder savedOrder = orderRepository.save(received);
        if (order.status() != savedOrder.status()) {
            auditStatusChange(savedOrder, order.status(), savedOrder.status(), "RECEIVED");
        }
        auditPurchaseOrder(savedOrder, fullyReceived ? "PURCHASE_ORDER_RECEIVED_FULL" : "PURCHASE_ORDER_RECEIVED_PARTIAL",
                "Reception commande fournisseur, statut " + savedOrder.status());
        return savedOrder;
    }

    public PurchaseOrder receive(Long id) {
        return receive(id, null);
    }

    public List<PurchaseOrderAuditLog> auditLogs(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        orderRepository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        return auditLogRepository.findByPurchaseOrder(id, organisationId);
    }

    private PurchaseOrder copyWithStatus(PurchaseOrder order, PurchaseOrderStatus status, LocalDateTime receivedAt) {
        return PurchaseOrder.builder()
                .id(order.id())
                .organisationId(order.organisationId())
                .supplierId(order.supplierId())
                .status(status)
                .expectedDeliveryDate(order.expectedDeliveryDate())
                .createdAt(order.createdAt())
                .receivedAt(receivedAt)
                .lines(order.lines())
                .build();
    }

    private PurchaseOrderLine validateLine(PurchaseOrderLine line, Long organisationId) {
        productRepository.findById(line.productId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.productId()));
        return PurchaseOrderLine.builder()
                .id(line.id())
                .productId(line.productId())
                .quantity(line.quantity())
                .receivedQuantity(0)
                .unitCost(line.unitCost())
                .build();
    }

    private PurchaseOrderLine receiveLine(PurchaseOrderLine line, Integer receivedQuantity) {
        int quantityToReceive = receivedQuantity == null ? 0 : receivedQuantity;
        if (quantityToReceive < 0) {
            throw new IllegalArgumentException("Received quantity must be positive");
        }
        if (quantityToReceive == 0) {
            return line;
        }

        int newReceivedQuantity = line.receivedQuantity() + quantityToReceive;
        if (newReceivedQuantity > line.quantity()) {
            throw new IllegalArgumentException("Received quantity exceeds ordered quantity");
        }

        stockService.registerMovement(line.productId(), quantityToReceive, "IN");

        return PurchaseOrderLine.builder()
                .id(line.id())
                .productId(line.productId())
                .quantity(line.quantity())
                .receivedQuantity(newReceivedQuantity)
                .unitCost(line.unitCost())
                .build();
    }

    private void requireCanPlaceOrder(PurchaseOrder order) {
        if (isAdmin()) {
            return;
        }
        BigDecimal approvalThreshold = approvalSettingsService.currentThreshold();
        if (orderTotal(order).compareTo(approvalThreshold) > 0) {
            throw new IllegalArgumentException("Purchase order requires admin approval above " + approvalThreshold);
        }
    }

    private void requireCanConfirm(PurchaseOrder order) {
        if (order.status() == PurchaseOrderStatus.APPROVED || isAdmin()) {
            return;
        }
        requireCanPlaceOrder(order);
    }

    private void requireCanCancel(PurchaseOrder order) {
        if (isAdmin() || order.status() == PurchaseOrderStatus.DRAFT || order.status() == PurchaseOrderStatus.APPROVED) {
            return;
        }
        throw new IllegalArgumentException("Only admins can cancel ordered purchase orders");
    }

    private void requireAdmin(String message) {
        if (!isAdmin()) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isAdmin() {
        return authenticatedUserProvider.requireUser().getRole() == Role.ADMIN;
    }

    private void auditStatusChange(
            PurchaseOrder order,
            PurchaseOrderStatus previousStatus,
            PurchaseOrderStatus newStatus,
            String action
    ) {
        User user = authenticatedUserProvider.requireUser();
        auditLogRepository.save(PurchaseOrderAuditLog.builder()
                .organisationId(order.organisationId())
                .purchaseOrderId(order.id())
                .action(action)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole())
                .orderTotal(orderTotal(order))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void auditPurchaseOrder(PurchaseOrder order, String action, String summary) {
        auditLogService.record(authenticatedUserProvider.requireUser(), action, "PURCHASE_ORDER", order.id(), "BACKEND", summary);
    }

    private BigDecimal orderTotal(PurchaseOrder order) {
        return order.lines().stream()
                .map(line -> (line.unitCost() == null ? BigDecimal.ZERO : line.unitCost())
                        .multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PurchaseOrderApprovalItem toApprovalItem(
            PurchaseOrder order,
            Supplier supplier,
            Map<Long, AiStockoutRiskEntity> risksByProduct
    ) {
        List<AiStockoutRiskEntity> lineRisks = order.lines().stream()
                .map(line -> risksByProduct.get(line.productId()))
                .filter(risk -> risk != null)
                .toList();
        Optional<AiStockoutRiskEntity> maxRisk = lineRisks.stream()
                .max(Comparator.comparing(AiStockoutRiskEntity::getRiskScore));
        LocalDate earliestStockout = lineRisks.stream()
                .map(AiStockoutRiskEntity::getEstimatedStockoutDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        BigDecimal maxRiskScore = maxRisk.map(AiStockoutRiskEntity::getRiskScore).orElse(BigDecimal.ZERO);
        String maxRiskLevel = maxRisk.map(AiStockoutRiskEntity::getRiskLevel).orElse("UNKNOWN");

        return PurchaseOrderApprovalItem.builder()
                .id(order.id())
                .supplierId(order.supplierId())
                .supplierName(supplier == null ? "Supplier deleted" : supplier.name())
                .status(order.status())
                .orderTotal(orderTotal(order))
                .linesCount(order.lines().size())
                .totalQuantity(order.lines().stream().mapToInt(PurchaseOrderLine::quantity).sum())
                .maxRiskScore(maxRiskScore)
                .maxRiskLevel(maxRiskLevel)
                .earliestStockoutDate(earliestStockout)
                .urgency(urgency(maxRiskScore, earliestStockout))
                .riskReason(maxRisk.map(AiStockoutRiskEntity::getReason).orElse("No AI risk linked to this order"))
                .expectedDeliveryDate(order.expectedDeliveryDate())
                .createdAt(order.createdAt())
                .build();
    }

    private String urgency(BigDecimal riskScore, LocalDate earliestStockout) {
        long daysToStockout = earliestStockout == null ? Long.MAX_VALUE : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), earliestStockout);
        if (riskScore.compareTo(BigDecimal.valueOf(70)) >= 0 || daysToStockout <= 7) {
            return "CRITICAL";
        }
        if (riskScore.compareTo(BigDecimal.valueOf(35)) >= 0 || daysToStockout <= 30) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private int urgencyRank(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            default -> 1;
        };
    }

    private void notifyIfAboveApprovalThreshold(PurchaseOrder order) {
        BigDecimal threshold = approvalSettingsService.currentThreshold();
        BigDecimal total = orderTotal(order);
        if (total.compareTo(threshold) <= 0) {
            return;
        }
        notificationService.createOnce(
                order.organisationId(),
                "PURCHASE_ORDER_THRESHOLD",
                "WARNING",
                "Commande au-dessus du seuil",
                "La commande #" + order.id() + " atteint " + total + " et depasse le seuil " + threshold + ".",
                order.id(),
                order.supplierId(),
                "purchase-order-threshold-" + order.id()
        );
    }

    private void notifyIfCriticalPendingOrder(PurchaseOrderApprovalItem item) {
        if (!"CRITICAL".equals(item.urgency())) {
            return;
        }
        notificationService.createOnce(
                TenantContext.requireOrganisationId(),
                "AI_CRITICAL_STOCKOUT",
                "CRITICAL",
                "Rupture critique liee a une commande",
                "La commande #" + item.id() + " pour " + item.supplierName()
                        + " couvre un produit avec risque IA " + item.maxRiskLevel()
                        + " et rupture estimee " + (item.earliestStockoutDate() == null ? "inconnue" : item.earliestStockoutDate()) + ".",
                item.id(),
                item.supplierId(),
                "ai-critical-pending-order-" + item.id()
        );
    }
}
