package com.gestionstock.ai.application.service;

import com.gestionstock.ai.application.dto.*;
import com.gestionstock.ai.infrastructure.entity.AiForecastEntity;
import com.gestionstock.ai.infrastructure.entity.AiReorderRecommendationEntity;
import com.gestionstock.ai.infrastructure.entity.AiStockoutRiskEntity;
import com.gestionstock.ai.infrastructure.entity.AiAuditLogEntity;
import com.gestionstock.ai.infrastructure.entity.AiCopilotConversationEntity;
import com.gestionstock.ai.infrastructure.entity.AiCopilotMessageEntity;
import com.gestionstock.ai.infrastructure.client.AiEngineClient;
import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.procurement.application.dto.PurchaseOrderResponse;
import com.gestionstock.procurement.application.mapper.PurchaseOrderMapper;
import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.repository.PurchaseOrderRepository;
import com.gestionstock.procurement.domain.repository.SupplierRepository;
import com.gestionstock.procurement.domain.service.ProductSupplierService;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.model.StockMovement;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiExperienceService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final AiForecastRepository forecastRepository;
    private final AiStockoutRiskRepository riskRepository;
    private final AiReorderRecommendationRepository recommendationRepository;
    private final AiInsightRepository insightRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSupplierService productSupplierService;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final AiDecisionService aiDecisionService;
    private final AiEngineClient aiEngineClient;
    private final AiCopilotConversationRepository conversationRepository;
    private final AiCopilotMessageRepository messageRepository;
    private final AiAuditLogRepository auditLogRepository;
    private final AuthenticatedUserProvider userProvider;
    private final UserRepository userRepository;

    public AiExecutiveDashboardResponse executiveDashboard() {
        Long organisationId = TenantContext.requireOrganisationId();
        AiDashboardResponse dashboard = aiDecisionService.getDashboard();
        List<AiProductHealthResponse> health = stockHealth();
        List<PurchaseOrder> pendingOrders = purchaseOrderRepository.findAll(organisationId).stream()
                .filter(order -> order.status() == PurchaseOrderStatus.DRAFT
                        || order.status() == PurchaseOrderStatus.APPROVED
                        || order.status() == PurchaseOrderStatus.ORDERED)
                .toList();
        List<SupplierIssueResponse> supplierIssues = supplierIssues(organisationId, pendingOrders);
        Map<Long, Supplier> suppliers = supplierRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Supplier::id, Function.identity()));
        Map<Long, Product> products = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        return new AiExecutiveDashboardResponse(
                health.size(),
                (int) health.stream().filter(item -> item.currentStock() <= item.minStock()).count(),
                (int) health.stream().filter(item -> "HIGH".equals(item.riskLevel())).count(),
                (int) dashboard.reorderRecommendations().stream().filter(item -> item.purchaseOrderId() == null).count(),
                pendingOrders.size(),
                supplierIssues.size(),
                round(health.stream().mapToDouble(AiProductHealthResponse::healthScore).average().orElse(100)),
                health.stream().sorted(Comparator.comparing(AiProductHealthResponse::healthScore)).limit(6).toList(),
                dashboard.reorderRecommendations().stream().limit(8).toList(),
                pendingOrders.stream()
                        .sorted(Comparator.comparing(PurchaseOrder::createdAt).reversed())
                        .limit(8)
                        .map(order -> purchaseOrderMapper.toResponse(order, suppliers.get(order.supplierId()), products))
                        .toList(),
                supplierIssues,
                dashboard.insights().stream().limit(5).toList()
        );
    }

    public List<AiProductHealthResponse> stockHealth() {
        Long organisationId = TenantContext.requireOrganisationId();
        Map<Long, Stock> stockByProductId = stockRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Stock::productId, Function.identity()));
        Map<Long, AiStockoutRiskEntity> riskByProductId = riskRepository.findByOrganisationIdOrderByRiskScoreDesc(organisationId).stream()
                .collect(Collectors.toMap(AiStockoutRiskEntity::getProductId, Function.identity(), (first, second) -> first));
        Map<Long, AiForecastEntity> forecast30ByProductId = forecastRepository.findByOrganisationIdAndHorizonDaysOrderByGeneratedAtDesc(organisationId, 30).stream()
                .collect(Collectors.toMap(AiForecastEntity::getProductId, Function.identity(), (first, second) -> first));
        Map<Long, AiReorderRecommendationEntity> recommendationByProductId = recommendationRepository.findByOrganisationIdOrderByRecommendedQuantityDesc(organisationId).stream()
                .collect(Collectors.toMap(AiReorderRecommendationEntity::getProductId, Function.identity(), (first, second) -> first));
        List<PurchaseOrder> openOrders = purchaseOrderRepository.findAll(organisationId).stream()
                .filter(order -> order.status() == PurchaseOrderStatus.DRAFT
                        || order.status() == PurchaseOrderStatus.APPROVED
                        || order.status() == PurchaseOrderStatus.ORDERED)
                .toList();
        Map<Long, List<StockMovement>> movementsByProduct = movementRepository.findRecent(organisationId, 600).stream()
                .collect(Collectors.groupingBy(StockMovement::productId));

        return productRepository.findAll(organisationId).stream()
                .map(product -> healthFor(product, stockByProductId, movementsByProduct, riskByProductId,
                        forecast30ByProductId, recommendationByProductId, openOrders, organisationId))
                .sorted(Comparator.comparing(AiProductHealthResponse::healthScore))
                .toList();
    }

    public AiWhatIfResponse whatIf(AiWhatIfRequest request) {
        Long organisationId = TenantContext.requireOrganisationId();
        Product product = productRepository.findById(request.productId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        int currentStock = stockRepository.findByProduct(product.id(), organisationId).map(Stock::quantity).orElse(0);
        double demand = Math.max(0.1, dailyDemand(movementRepository.findHistory(organisationId, product.id(), null, 0, 250)));
        double currentCoverage = currentStock / demand;
        int projectedStock = (int) Math.floor(currentStock - (demand * request.leadTimeDays()) + request.orderQuantity());
        double projectedCoverage = Math.max(0, projectedStock) / demand;
        String recommendation = projectedCoverage < 14
                ? "Couverture encore faible: augmente la commande ou reduis le lead time fournisseur."
                : projectedCoverage < 30
                ? "Couverture correcte pour le court terme, a surveiller si la demande accelere."
                : "Couverture confortable apres reception.";

        return new AiWhatIfResponse(
                product.id(),
                product.name(),
                currentStock,
                request.orderQuantity(),
                request.leadTimeDays(),
                round(demand),
                round(currentCoverage),
                round(projectedCoverage),
                projectedStock,
                recommendation
        );
    }

    public AiCopilotResponse copilot(AiCopilotRequest request) {
        String question = request.question().toLowerCase();
        AiExecutiveDashboardResponse dashboard = executiveDashboard();
        Long organisationId = TenantContext.requireOrganisationId();
        User user = userProvider.requireUser();
        AiCopilotConversationEntity conversation = resolveConversation(request.conversationId(), organisationId, user);
        AiCopilotResponse localResponse = withConversation(localCopilotResponse(question, dashboard), conversation.getId());
        AiCopilotResponse response = aiEngineClient.answerCopilot(new AiEngineClient.CopilotRequest(
                        organisationId,
                        request.question(),
                        copilotContext(dashboard)
                ))
                .map(engineResponse -> new AiCopilotResponse(
                        conversation.getId(),
                        engineResponse.answer(),
                        engineResponse.bullets(),
                        engineResponse.relatedProductIds(),
                        toCitations(engineResponse.citations()),
                        engineResponse.source(),
                        suggestedActions(request.question().toLowerCase(), dashboard)
                ))
                .orElse(localResponse);
        saveCopilotMessage(conversation, user, request.question(), response);
        audit(user, "COPILOT_ASK", "COPILOT_CONVERSATION", conversation.getId(), response.source(), response.answer());
        return response;
    }

    public AiRecommendationExplanationResponse explainRecommendation(Long recommendationId) {
        Long organisationId = TenantContext.requireOrganisationId();
        AiReorderRecommendationResponse recommendation = aiDecisionService.getReorderRecommendations().stream()
                .filter(item -> item.id().equals(recommendationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
        AiProductHealthResponse health = stockHealth().stream()
                .filter(item -> item.productId().equals(recommendation.productId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product health not found"));
        AiRecommendationExplanationResponse fallback = localExplanation(recommendation, health);

        User user = userProvider.requireUser();
        AiRecommendationExplanationResponse response = aiEngineClient.explainRecommendation(new AiEngineClient.RecommendationExplanationRequest(
                        organisationId,
                        recommendation.id(),
                        recommendation.productName(),
                        recommendation.sku(),
                        recommendation.recommendedQuantity(),
                        health.currentStock(),
                        health.minStock(),
                        BigDecimal.valueOf(health.averageDailyDemand()),
                        BigDecimal.valueOf(health.stockCoverageDays()),
                        BigDecimal.valueOf(health.riskScore()),
                        health.riskLevel(),
                        recommendation.preferredSupplierName(),
                        recommendation.preferredSupplierScore() == null ? null : BigDecimal.valueOf(recommendation.preferredSupplierScore()),
                        recommendation.preferredSupplierScoreExplanation(),
                        recommendation.reason()
                ))
                .map(engineResponse -> new AiRecommendationExplanationResponse(
                        engineResponse.recommendationId(),
                        engineResponse.summary(),
                        engineResponse.drivers(),
                        engineResponse.risks(),
                        engineResponse.nextAction(),
                        explanationCitations(recommendation, health),
                        engineResponse.source()
                ))
                .orElse(fallback);
        audit(user, "RECOMMENDATION_EXPLAIN", "AI_REORDER_RECOMMENDATION", recommendationId, response.source(), response.summary());
        return response;
    }

    public List<AiCopilotConversationResponse> copilotHistory() {
        Long organisationId = TenantContext.requireOrganisationId();
        User user = userProvider.requireUser();
        Long userId = requireUserId(user);
        return conversationRepository.findTop20ByOrganisationIdAndUserIdOrderByUpdatedAtDesc(organisationId, userId).stream()
                .map(conversation -> new AiCopilotConversationResponse(
                        conversation.getId(),
                        conversation.getTitle(),
                        conversation.getUpdatedAt(),
                        messageRepository.findByOrganisationIdAndConversationIdOrderByCreatedAtAsc(organisationId, conversation.getId()).stream()
                                .map(this::toMessageResponse)
                                .toList()
                ))
                .toList();
    }

    public List<AiAuditLogResponse> auditLogs(
            String action,
            String actorEmail,
            String targetType,
            String source,
            String module,
            String severity,
            LocalDate from,
            LocalDate to
    ) {
        return filteredAuditLogs(action, actorEmail, targetType, source, module, severity, from, to)
                .stream()
                .limit(100)
                .map(this::toAuditLogResponse)
                .toList();
    }

    public byte[] exportAuditLogsCsv(
            String action,
            String actorEmail,
            String targetType,
            String source,
            String module,
            String severity,
            LocalDate from,
            LocalDate to
    ) {
        List<AiAuditLogResponse> logs = filteredAuditLogs(action, actorEmail, targetType, source, module, severity, from, to)
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("id,createdAt,actorEmail,module,severity,action,targetType,targetId,source,summary")
                .append(System.lineSeparator());
        for (AiAuditLogResponse log : logs) {
            csv.append(log.id()).append(',')
                    .append(csv(log.createdAt())).append(',')
                    .append(csv(log.actorEmail())).append(',')
                    .append(csv(auditModule(log.action(), log.targetType()))).append(',')
                    .append(csv(auditSeverity(log.action(), log.source(), log.summary()))).append(',')
                    .append(csv(log.action())).append(',')
                    .append(csv(log.targetType())).append(',')
                    .append(log.targetId() == null ? "" : log.targetId()).append(',')
                    .append(csv(log.source())).append(',')
                    .append(csv(log.summary()))
                    .append(System.lineSeparator());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportAuditLogsPdf(
            String action,
            String actorEmail,
            String targetType,
            String source,
            String module,
            String severity,
            LocalDate from,
            LocalDate to
    ) {
        List<AiAuditLogResponse> logs = filteredAuditLogs(action, actorEmail, targetType, source, module, severity, from, to)
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream content = null;
            try {
                content = new PDPageContentStream(document, page);
                content.setFont(PDType1Font.HELVETICA, 10);
                float y = 760f;
                y = writePdfLine(content, "StockPilot AI - Audit logs", y, 18);
                y = writePdfLine(content, "Generated at " + LocalDateTime.now() + " | Events: " + logs.size(), y, 16);
                y = writePdfLine(content, "Date | Module | Severity | Actor | Action | Target | Summary", y, 16);

                for (AiAuditLogResponse log : logs) {
                    if (y < 42) {
                        content.close();
                        page = new PDPage();
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        content.setFont(PDType1Font.HELVETICA, 10);
                        y = writePdfLine(content, "Date | Module | Severity | Actor | Action | Target | Summary", 760f, 16);
                    }
                    String line = log.createdAt() + " | "
                            + auditModule(log.action(), log.targetType()) + " | "
                            + auditSeverity(log.action(), log.source(), log.summary()) + " | "
                            + log.actorEmail() + " | "
                            + log.action() + " | "
                            + log.targetType() + (log.targetId() == null ? "" : "#" + log.targetId()) + " | "
                            + log.summary();
                    y = writePdfLine(content, truncatePdf(line), y, 14);
                }
            } finally {
                if (content != null) {
                    content.close();
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Audit PDF generation failed", exception);
        }
    }

    private List<AiAuditLogEntity> filteredAuditLogs(
            String action,
            String actorEmail,
            String targetType,
            String source,
            String module,
            String severity,
            LocalDate from,
            LocalDate to
    ) {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can read audit logs");
        }
        Long organisationId = TenantContext.requireOrganisationId();
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();
        return auditLogRepository.findTop500ByOrganisationIdOrderByCreatedAtDesc(organisationId).stream()
                .filter(log -> matchesFilter(action, log.getAction()))
                .filter(log -> matchesFilter(actorEmail, log.getActorEmail()))
                .filter(log -> matchesFilter(targetType, log.getTargetType()))
                .filter(log -> matchesFilter(source, log.getSource()))
                .filter(log -> matchesFilter(module, auditModule(log.getAction(), log.getTargetType())))
                .filter(log -> matchesFilter(severity, auditSeverity(log.getAction(), log.getSource(), log.getSummary())))
                .filter(log -> fromDateTime == null || !log.getCreatedAt().isBefore(fromDateTime))
                .filter(log -> toDateTime == null || log.getCreatedAt().isBefore(toDateTime))
                .toList();
    }

    private AiAuditLogResponse toAuditLogResponse(AiAuditLogEntity log) {
        return new AiAuditLogResponse(
                log.getId(),
                log.getActorEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSource(),
                log.getSummary(),
                log.getCreatedAt()
        );
    }

    private boolean matchesFilter(String filter, String value) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase().contains(filter.trim().toLowerCase());
    }

    private String auditModule(String action, String targetType) {
        String value = ((action == null ? "" : action) + " " + (targetType == null ? "" : targetType)).toUpperCase();
        if (value.contains("AUTH") || value.contains("USER")) {
            return "AUTH";
        }
        if (value.contains("PURCHASE_ORDER") || value.contains("SUPPLIER") || value.contains("APPROVAL")) {
            return "PROCUREMENT";
        }
        if (value.contains("NOTIFICATION")) {
            return "NOTIFICATION";
        }
        if (value.contains("IMPORT")) {
            return "IMPORT";
        }
        if (value.contains("COPILOT") || value.contains("RECOMMENDATION") || value.contains("AI_")) {
            return "AI";
        }
        if (value.contains("PRODUCT")) {
            return "PRODUCT";
        }
        return "SYSTEM";
    }

    private String auditSeverity(String action, String source, String summary) {
        String value = ((action == null ? "" : action) + " " + (source == null ? "" : source) + " " + (summary == null ? "" : summary)).toUpperCase();
        if (value.contains("CRITICAL") || value.contains("FAILED") || value.contains("CANCELLED")) {
            return "CRITICAL";
        }
        if (value.contains("WARNING") || value.contains("THRESHOLD") || value.contains("DISMISSED")) {
            return "WARNING";
        }
        return "INFO";
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String truncatePdf(String value) {
        String normalized = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
        return normalized.length() <= 150 ? normalized : normalized.substring(0, 150);
    }

    private float writePdfLine(PDPageContentStream content, String text, float y, float lineHeight) throws IOException {
        content.beginText();
        content.newLineAtOffset(40, y);
        content.showText(text == null ? "" : text.replace("\u0000", ""));
        content.endText();
        return y - lineHeight;
    }

    private AiCopilotResponse localCopilotResponse(String question, AiExecutiveDashboardResponse dashboard) {
        List<AiCitationResponse> citations = dashboard.topRisks().stream()
                .limit(4)
                .map(item -> new AiCitationResponse("STOCK_HEALTH", item.productName(), item.productId(), item.supplierId(), item.purchaseOrderId()))
                .toList();
        if (question.contains("fournisseur")) {
            List<String> bullets = dashboard.supplierIssues().stream()
                    .limit(4)
                    .map(issue -> issue.supplierName() + ": " + issue.reason())
                    .toList();
            return new AiCopilotResponse(null, "Les fournisseurs a traiter en priorite sont ceux avec commandes ouvertes, delais longs ou score degrade.", bullets, List.of(), supplierCitations(dashboard), "LOCAL_FALLBACK", suggestedActions(question, dashboard));
        }
        if (question.contains("commande") || question.contains("acheter") || question.contains("recommander")) {
            List<String> bullets = dashboard.recommendations().stream()
                    .limit(5)
                    .map(recommendation -> recommendation.productName() + ": commander " + recommendation.recommendedQuantity()
                            + " unite(s), fournisseur IA " + fallback(recommendation.preferredSupplierName(), "a selectionner"))
                    .toList();
            return new AiCopilotResponse(null, "Voici les commandes IA les plus utiles a transformer en brouillon.", bullets,
                    dashboard.recommendations().stream().limit(5).map(AiReorderRecommendationResponse::productId).toList(),
                    recommendationCitations(dashboard),
                    "LOCAL_FALLBACK",
                    suggestedActions(question, dashboard));
        }
        if (question.contains("rupture") || question.contains("risque") || question.contains("critique")) {
            List<String> bullets = dashboard.topRisks().stream()
                    .limit(5)
                    .map(risk -> risk.productName() + ": score " + risk.riskScore() + ", couverture " + risk.stockCoverageDays() + " jours")
                    .toList();
            return new AiCopilotResponse(null, "Les risques de rupture les plus urgents sont classes par score sante stock.", bullets,
                    dashboard.topRisks().stream().limit(5).map(AiProductHealthResponse::productId).toList(),
                    citations,
                    "LOCAL_FALLBACK",
                    suggestedActions(question, dashboard));
        }
        List<String> bullets = List.of(
                dashboard.criticalStockCount() + " produit(s) sous seuil critique",
                dashboard.pendingRecommendationsCount() + " recommandation(s) IA sans commande",
                dashboard.pendingPurchaseOrdersCount() + " commande(s) fournisseur en attente",
                "Score sante moyen: " + dashboard.stockHealthAverage()
        );
        return new AiCopilotResponse(null, "Synthese StockPilot AI: priorise les ruptures, transforme les recommandations en commandes, puis traite les fournisseurs a probleme.", bullets,
                dashboard.topRisks().stream().limit(3).map(AiProductHealthResponse::productId).toList(),
                citations,
                "LOCAL_FALLBACK",
                suggestedActions(question, dashboard));
    }

    private List<AiCopilotActionResponse> suggestedActions(String question, AiExecutiveDashboardResponse dashboard) {
        List<AiCopilotActionResponse> actions = new java.util.ArrayList<>();
        if (question.contains("commande") || question.contains("acheter") || question.contains("recommander") || question.contains("aujourd")) {
            dashboard.recommendations().stream()
                    .filter(recommendation -> recommendation.purchaseOrderId() == null)
                    .limit(3)
                    .map(recommendation -> new AiCopilotActionResponse(
                            "CREATE_PURCHASE_ORDER",
                            "Creer une commande",
                            "Transformer la recommandation IA pour " + recommendation.productName() + " en commande DRAFT.",
                            recommendation.productId(),
                            recommendation.id(),
                            null,
                            recommendation.preferredSupplierId(),
                            recommendation.recommendedQuantity(),
                            recommendation.leadTimeDays(),
                            "/procurement",
                            true
                    ))
                    .forEach(actions::add);
        }
        if (question.contains("rupture") || question.contains("risque") || question.contains("critique") || question.contains("simulation")) {
            dashboard.topRisks().stream()
                    .limit(3)
                    .map(risk -> new AiCopilotActionResponse(
                            "RUN_WHAT_IF",
                            "Simuler une commande",
                            "Tester l'impact d'une commande sur la couverture de " + risk.productName() + ".",
                            risk.productId(),
                            risk.recommendationId(),
                            risk.purchaseOrderId(),
                            risk.supplierId(),
                            Math.max(risk.recommendedQuantity(), 1),
                            7,
                            "/stock-health",
                            false
                    ))
                    .forEach(actions::add);
        }
        if (question.contains("expli") || question.contains("pourquoi") || question.contains("reco")) {
            dashboard.recommendations().stream()
                    .limit(3)
                    .map(recommendation -> new AiCopilotActionResponse(
                            "EXPLAIN_RECOMMENDATION",
                            "Expliquer la recommandation",
                            "Afficher les facteurs IA derriere la recommandation pour " + recommendation.productName() + ".",
                            recommendation.productId(),
                            recommendation.id(),
                            recommendation.purchaseOrderId(),
                            recommendation.preferredSupplierId(),
                            recommendation.recommendedQuantity(),
                            recommendation.leadTimeDays(),
                            "/prediction",
                            false
                    ))
                    .forEach(actions::add);
        }
        if (question.contains("fournisseur")) {
            dashboard.supplierIssues().stream()
                    .limit(3)
                    .map(issue -> new AiCopilotActionResponse(
                            "OPEN_SUPPLIER_360",
                            "Voir fournisseur 360",
                            "Ouvrir la fiche fournisseur pour " + issue.supplierName() + ".",
                            null,
                            null,
                            null,
                            issue.supplierId(),
                            null,
                            issue.leadTimeDays(),
                            "/suppliers/" + issue.supplierId() + "/360",
                            false
                    ))
                    .forEach(actions::add);
        }
        if (actions.isEmpty()) {
            actions.add(new AiCopilotActionResponse(
                    "OPEN_AI_DASHBOARD",
                    "Ouvrir le cockpit IA",
                    "Voir les risques, recommandations et fournisseurs a probleme.",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "/ai-dashboard",
                    false
            ));
        }
        return actions.stream().limit(6).toList();
    }

    private List<AiEngineClient.CopilotContextItem> copilotContext(AiExecutiveDashboardResponse dashboard) {
        List<AiEngineClient.CopilotContextItem> items = new java.util.ArrayList<>();
        dashboard.topRisks().forEach(item -> items.add(new AiEngineClient.CopilotContextItem(
                "STOCK_HEALTH",
                item.productName(),
                "SKU " + item.sku() + ", stock " + item.currentStock() + ", seuil " + item.minStock()
                        + ", risque " + item.riskLevel() + ", score sante " + item.healthScore()
                        + ", action " + item.actionRecommendation(),
                item.productId(),
                item.supplierId(),
                item.purchaseOrderId()
        )));
        dashboard.recommendations().forEach(item -> items.add(new AiEngineClient.CopilotContextItem(
                "REORDER_RECOMMENDATION",
                item.productName(),
                "Commander " + item.recommendedQuantity() + " unite(s), statut " + item.status()
                        + ", fournisseur " + fallback(item.preferredSupplierName(), "non defini")
                        + ", raison " + item.reason(),
                item.productId(),
                item.preferredSupplierId(),
                item.purchaseOrderId()
        )));
        dashboard.supplierIssues().forEach(item -> items.add(new AiEngineClient.CopilotContextItem(
                "SUPPLIER_ISSUE",
                item.supplierName(),
                item.reason() + ", score probleme " + item.issueScore(),
                null,
                item.supplierId(),
                null
        )));
        dashboard.pendingOrders().forEach(item -> items.add(new AiEngineClient.CopilotContextItem(
                "PURCHASE_ORDER",
                "Commande #" + item.id(),
                "Fournisseur " + item.supplierName() + ", statut " + item.status() + ", lignes " + item.lines().size(),
                null,
                item.supplierId(),
                item.id()
        )));
        return items;
    }

    private AiRecommendationExplanationResponse localExplanation(AiReorderRecommendationResponse recommendation, AiProductHealthResponse health) {
        List<String> drivers = new java.util.ArrayList<>(List.of(
                "Stock actuel " + health.currentStock() + " pour un seuil minimum " + health.minStock() + ".",
                "Demande moyenne " + health.averageDailyDemand() + " unite/jour et couverture estimee " + health.stockCoverageDays() + " jours.",
                "Quantite recommandee " + recommendation.recommendedQuantity() + " unite(s)."
        ));
        if (recommendation.preferredSupplierName() != null) {
            drivers.add("Fournisseur IA: " + recommendation.preferredSupplierName() + ", score " + recommendation.preferredSupplierScore() + ".");
        }
        List<String> risks = new java.util.ArrayList<>();
        if ("HIGH".equals(health.riskLevel())) {
            risks.add("Risque de rupture eleve si la commande reste en attente.");
        }
        if (health.stockCoverageDays() < 7) {
            risks.add("Couverture inferieure a une semaine.");
        }
        if (recommendation.preferredSupplierName() == null) {
            risks.add("Aucun fournisseur optimal configure pour ce produit.");
        }
        return new AiRecommendationExplanationResponse(
                recommendation.id(),
                "Commander " + recommendation.recommendedQuantity() + " unite(s) de " + recommendation.productName()
                        + " pour reduire un risque " + health.riskLevel() + ".",
                drivers,
                risks.isEmpty() ? List.of("Aucun risque secondaire majeur detecte.") : risks,
                "Transformer la recommandation IA en commande brouillon puis verifier le fournisseur et la quantite.",
                explanationCitations(recommendation, health),
                "LOCAL_FALLBACK"
        );
    }

    private AiProductHealthResponse healthFor(
            Product product,
            Map<Long, Stock> stockByProductId,
            Map<Long, List<StockMovement>> movementsByProduct,
            Map<Long, AiStockoutRiskEntity> riskByProductId,
            Map<Long, AiForecastEntity> forecast30ByProductId,
            Map<Long, AiReorderRecommendationEntity> recommendationByProductId,
            List<PurchaseOrder> openOrders,
            Long organisationId
    ) {
        int currentStock = stockByProductId.getOrDefault(product.id(), Stock.builder().productId(product.id()).quantity(0).build()).quantity();
        double demand = dailyDemand(movementsByProduct.getOrDefault(product.id(), List.of()));
        double coverage = demand <= 0 ? 999 : currentStock / demand;
        AiStockoutRiskEntity risk = riskByProductId.get(product.id());
        AiForecastEntity forecast = forecast30ByProductId.get(product.id());
        AiReorderRecommendationEntity recommendation = recommendationByProductId.get(product.id());
        ProductSupplierService.SupplierScore supplierScore = productSupplierService.findBestScoreForProduct(product.id(), organisationId).orElse(null);
        int openPoCount = (int) openOrders.stream()
                .filter(order -> order.lines().stream().anyMatch(line -> line.productId().equals(product.id())))
                .count();
        double numericRisk = risk == null ? 0 : risk.getRiskScore().doubleValue();
        double healthScore = Math.max(0, Math.min(100,
                100 - numericRisk - (currentStock <= minStock(product) ? 18 : 0) - (coverage < 14 ? 10 : 0)
                        + (supplierScore == null ? 0 : Math.max(0, 15 - supplierScore.score() / 6))
                        + Math.min(8, openPoCount * 4)));

        String action = recommendation != null && recommendation.getPurchaseOrderId() == null
                ? "Transformer la recommandation IA en commande brouillon"
                : currentStock <= minStock(product)
                ? "Verifier le stock physique et relancer le fournisseur"
                : "Surveiller la tendance demande";

        return new AiProductHealthResponse(
                product.id(),
                product.name(),
                product.sku(),
                product.category(),
                currentStock,
                minStock(product),
                round(demand),
                forecast == null ? 0 : forecast.getPredictedQuantity().intValue(),
                coverage == 999 ? 999 : round(coverage),
                round(numericRisk),
                risk == null ? "LOW" : risk.getRiskLevel(),
                recommendation == null ? 0 : recommendation.getRecommendedQuantity(),
                recommendation == null ? null : recommendation.getId(),
                recommendation == null ? null : recommendation.getPurchaseOrderId(),
                supplierScore == null ? null : supplierScore.supplier().id(),
                supplierScore == null ? null : supplierScore.supplier().name(),
                supplierScore == null ? null : supplierScore.score(),
                openPoCount,
                round(healthScore),
                action
        );
    }

    private List<SupplierIssueResponse> supplierIssues(Long organisationId, List<PurchaseOrder> pendingOrders) {
        Map<Long, Long> openOrdersBySupplier = pendingOrders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::supplierId, Collectors.counting()));
        return supplierRepository.findAll(organisationId).stream()
                .map(supplier -> {
                    long openOrders = openOrdersBySupplier.getOrDefault(supplier.id(), 0L);
                    double score = Math.min(100, supplier.leadTimeDays() * 4.0 + openOrders * 12.0);
                    String reason = openOrders > 0
                            ? openOrders + " commande(s) ouverte(s), lead time " + supplier.leadTimeDays() + " jours"
                            : "Lead time " + supplier.leadTimeDays() + " jours";
                    return new SupplierIssueResponse(supplier.id(), supplier.name(), supplier.leadTimeDays(), openOrders, round(score), reason);
                })
                .filter(issue -> issue.issueScore() >= 24 || issue.openOrders() > 0)
                .sorted(Comparator.comparing(SupplierIssueResponse::issueScore).reversed())
                .limit(6)
                .toList();
    }

    private double dailyDemand(List<StockMovement> movements) {
        int outQuantity = movements.stream()
                .filter(movement -> movement.type() == MovementType.OUT)
                .mapToInt(StockMovement::quantity)
                .sum();
        long activeDays = Math.max(7, movements.stream()
                .map(StockMovement::createdAt)
                .min(Comparator.naturalOrder())
                .map(first -> Math.max(1, java.time.Duration.between(first, LocalDateTime.now()).toDays()))
                .orElse(7L));
        return outQuantity / (double) activeDays;
    }

    private int minStock(Product product) {
        return product.minStock() == null ? 0 : product.minStock();
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private AiCopilotConversationEntity resolveConversation(Long requestedId, Long organisationId, User user) {
        Long userId = requireUserId(user);
        if (requestedId != null) {
            return conversationRepository.findByIdAndOrganisationIdAndUserId(requestedId, organisationId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Copilot conversation not found"));
        }
        LocalDateTime now = LocalDateTime.now();
        return conversationRepository.save(AiCopilotConversationEntity.builder()
                .organisationId(organisationId)
                .userId(userId)
                .title("Conversation " + now.toLocalDate())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private AiCopilotResponse withConversation(AiCopilotResponse response, Long conversationId) {
        return new AiCopilotResponse(
                conversationId,
                response.answer(),
                response.bullets(),
                response.relatedProductIds(),
                response.citations(),
                response.source(),
                response.actions()
        );
    }

    private void saveCopilotMessage(AiCopilotConversationEntity conversation, User user, String question, AiCopilotResponse response) {
        LocalDateTime now = LocalDateTime.now();
        messageRepository.save(AiCopilotMessageEntity.builder()
                .organisationId(conversation.getOrganisationId())
                .conversationId(conversation.getId())
                .userId(requireUserId(user))
                .question(question)
                .answer(response.answer())
                .source(response.source())
                .citations(serializeCitations(response.citations()))
                .createdAt(now)
                .build());
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);
    }

    private AiCopilotMessageResponse toMessageResponse(AiCopilotMessageEntity message) {
        return new AiCopilotMessageResponse(
                message.getId(),
                message.getQuestion(),
                message.getAnswer(),
                message.getSource(),
                parseCitations(message.getCitations()),
                message.getCreatedAt()
        );
    }

    private void audit(User user, String action, String targetType, Long targetId, String source, String summary) {
        auditLogRepository.save(AiAuditLogEntity.builder()
                .organisationId(user.getOrganisation().getId())
                .userId(user.getId())
                .actorEmail(user.getEmail())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .source(source)
                .summary(summary == null ? "-" : summary.substring(0, Math.min(summary.length(), 1100)))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Long requireUserId(User user) {
        if (user.getId() != null) {
            return user.getId();
        }
        return userRepository.findByEmail(user.getEmail())
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user id not found"));
    }

    private List<AiCitationResponse> toCitations(List<AiEngineClient.CopilotCitation> citations) {
        if (citations == null) {
            return List.of();
        }
        return citations.stream()
                .map(item -> new AiCitationResponse(item.type(), item.label(), item.productId(), item.supplierId(), item.purchaseOrderId()))
                .toList();
    }

    private List<AiCitationResponse> supplierCitations(AiExecutiveDashboardResponse dashboard) {
        return dashboard.supplierIssues().stream()
                .limit(4)
                .map(item -> new AiCitationResponse("SUPPLIER", item.supplierName(), null, item.supplierId(), null))
                .toList();
    }

    private List<AiCitationResponse> recommendationCitations(AiExecutiveDashboardResponse dashboard) {
        return dashboard.recommendations().stream()
                .limit(5)
                .map(item -> new AiCitationResponse("REORDER_RECOMMENDATION", item.productName(), item.productId(), item.preferredSupplierId(), item.purchaseOrderId()))
                .toList();
    }

    private List<AiCitationResponse> explanationCitations(AiReorderRecommendationResponse recommendation, AiProductHealthResponse health) {
        return List.of(new AiCitationResponse("PRODUCT", recommendation.productName(), recommendation.productId(), health.supplierId(), recommendation.purchaseOrderId()));
    }

    private String serializeCitations(List<AiCitationResponse> citations) {
        if (citations == null || citations.isEmpty()) {
            return "";
        }
        return citations.stream()
                .map(item -> "%s;%s;%s;%s;%s".formatted(
                        nullToDash(item.type()),
                        nullToDash(item.label()),
                        item.productId() == null ? "-" : item.productId(),
                        item.supplierId() == null ? "-" : item.supplierId(),
                        item.purchaseOrderId() == null ? "-" : item.purchaseOrderId()
                ))
                .collect(Collectors.joining("|"));
    }

    private List<AiCitationResponse> parseCitations(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("\\|"))
                .map(item -> item.split(";", -1))
                .filter(parts -> parts.length == 5)
                .map(parts -> new AiCitationResponse(
                        dashToNull(parts[0]),
                        dashToNull(parts[1]),
                        parseLong(parts[2]),
                        parseLong(parts[3]),
                        parseLong(parts[4])
                ))
                .toList();
    }

    private String nullToDash(Object value) {
        return value == null ? "-" : value.toString().replace("|", " ").replace(";", " ");
    }

    private String dashToNull(String value) {
        return "-".equals(value) ? null : value;
    }

    private Long parseLong(String value) {
        try {
            return "-".equals(value) ? null : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
