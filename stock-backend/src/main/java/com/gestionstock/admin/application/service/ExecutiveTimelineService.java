package com.gestionstock.admin.application.service;

import com.gestionstock.admin.application.dto.ExecutiveTimelineActionResponse;
import com.gestionstock.admin.application.dto.ExecutiveTimelineItemResponse;
import com.gestionstock.admin.application.dto.ExecutiveTimelineResponse;
import com.gestionstock.ai.infrastructure.entity.AiAuditLogEntity;
import com.gestionstock.ai.infrastructure.entity.AiReorderRecommendationEntity;
import com.gestionstock.ai.infrastructure.entity.AiStockoutRiskEntity;
import com.gestionstock.ai.infrastructure.repository.AiAuditLogRepository;
import com.gestionstock.ai.infrastructure.repository.AiReorderRecommendationRepository;
import com.gestionstock.ai.infrastructure.repository.AiStockoutRiskRepository;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.notification.infrastructure.entity.AdminNotificationEntity;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationJpaRepository;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderEntity;
import com.gestionstock.procurement.infrastructure.repository.PurchaseOrderJpaRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ExecutiveTimelineService {

    private static final BigDecimal CRITICAL_RISK_SCORE = BigDecimal.valueOf(70);

    private final AuthenticatedUserProvider userProvider;
    private final AiAuditLogRepository auditLogRepository;
    private final AdminNotificationJpaRepository notificationRepository;
    private final PurchaseOrderJpaRepository purchaseOrderRepository;
    private final AiStockoutRiskRepository stockoutRiskRepository;
    private final AiReorderRecommendationRepository recommendationRepository;

    public ExecutiveTimelineResponse today() {
        return report(LocalDate.now());
    }

    public ExecutiveTimelineResponse report(LocalDate date) {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can access executive timeline");
        }

        Long organisationId = TenantContext.requireOrganisationId();
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.plusDays(1).atStartOfDay();

        List<AdminNotificationEntity> notifications = notificationRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId);
        List<PurchaseOrderEntity> purchaseOrders = purchaseOrderRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId);
        List<AiStockoutRiskEntity> risks = stockoutRiskRepository.findByOrganisationIdOrderByRiskScoreDesc(organisationId);
        List<AiReorderRecommendationEntity> recommendations = recommendationRepository.findByOrganisationIdOrderByRecommendedQuantityDesc(organisationId);

        List<ExecutiveTimelineItemResponse> items = new ArrayList<>();
        addAuditItems(organisationId, start, end, items);
        addNotificationItems(notifications, start, end, items);
        addPurchaseOrderItems(purchaseOrders, start, end, items);
        addRiskItems(risks, start, end, items);
        addRecommendationItems(recommendations, start, end, items);

        items = items.stream()
                .sorted(Comparator
                        .comparingInt(ExecutiveTimelineItemResponse::priorityScore).reversed()
                        .thenComparing(ExecutiveTimelineItemResponse::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(30)
                .toList();

        int criticalRisks = (int) risks.stream().filter(this::isCriticalRisk).count();
        int openNotifications = (int) notifications.stream()
                .filter(notification -> notification.getReadAt() == null || "OPEN".equalsIgnoreCase(notification.getStatus()))
                .count();
        int pendingOrders = (int) purchaseOrders.stream().filter(this::isPendingOrder).count();
        int pendingRecommendations = (int) recommendations.stream()
                .filter(recommendation -> recommendation.getPurchaseOrderId() == null)
                .count();

        return new ExecutiveTimelineResponse(
                reportDate,
                criticalRisks,
                openNotifications,
                pendingOrders,
                pendingRecommendations,
                executiveSummary(criticalRisks, openNotifications, pendingOrders, pendingRecommendations),
                keyDecisions(criticalRisks, openNotifications, pendingOrders, pendingRecommendations),
                items,
                actions(criticalRisks, openNotifications, pendingOrders, pendingRecommendations)
        );
    }

    public byte[] exportCsv(LocalDate date) {
        ExecutiveTimelineResponse report = report(date);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("date,priorityScore,type,severity,title,description,occurredAt,notificationId,productId,purchaseOrderId,recommendationId,supplierId")
                .append(System.lineSeparator());
        for (ExecutiveTimelineItemResponse item : report.items()) {
            csv.append(csv(report.date())).append(',')
                    .append(item.priorityScore()).append(',')
                    .append(csv(item.type())).append(',')
                    .append(csv(item.severity())).append(',')
                    .append(csv(item.title())).append(',')
                    .append(csv(item.description())).append(',')
                    .append(csv(item.occurredAt())).append(',')
                    .append(value(item.notificationId())).append(',')
                    .append(value(item.productId())).append(',')
                    .append(value(item.purchaseOrderId())).append(',')
                    .append(value(item.recommendationId())).append(',')
                    .append(value(item.supplierId()))
                    .append(System.lineSeparator());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(LocalDate date) {
        ExecutiveTimelineResponse report = report(date);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream content = null;
            try {
                content = new PDPageContentStream(document, page);
                content.setFont(PDType1Font.HELVETICA, 10);
                float y = 760f;
                y = writePdfLine(content, "StockPilot AI - Daily Report " + report.date(), y, 18);
                y = writePdfLine(content, report.executiveSummary(), y, 16);
                y = writePdfLine(content, "Metrics: risks=" + report.criticalRisks()
                        + ", notifications=" + report.openNotifications()
                        + ", orders=" + report.pendingOrders()
                        + ", recommendations=" + report.pendingRecommendations(), y, 16);
                y = writePdfLine(content, "Key decisions", y, 16);
                for (String decision : report.keyDecisions()) {
                    y = writePdfLine(content, "- " + truncatePdf(decision), y, 14);
                }
                y = writePdfLine(content, "Timeline", y, 18);
                for (ExecutiveTimelineItemResponse item : report.items()) {
                    if (y < 42) {
                        content.close();
                        page = new PDPage();
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        content.setFont(PDType1Font.HELVETICA, 10);
                        y = 760f;
                    }
                    String line = item.occurredAt() + " | P" + item.priorityScore() + " | "
                            + item.type() + " | " + item.severity() + " | " + item.title() + " | " + item.description();
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
            throw new IllegalStateException("Daily report PDF generation failed", exception);
        }
    }

    private void addAuditItems(Long organisationId, LocalDateTime start, LocalDateTime end, List<ExecutiveTimelineItemResponse> items) {
        auditLogRepository.findTop500ByOrganisationIdOrderByCreatedAtDesc(organisationId).stream()
                .filter(log -> isToday(log.getCreatedAt(), start, end))
                .limit(12)
                .map(this::fromAuditLog)
                .forEach(items::add);
    }

    private ExecutiveTimelineItemResponse fromAuditLog(AiAuditLogEntity log) {
        Long purchaseOrderId = "PURCHASE_ORDER".equalsIgnoreCase(log.getTargetType()) ? log.getTargetId() : null;
        return new ExecutiveTimelineItemResponse(
                "AUDIT",
                auditSeverity(log),
                priorityScore("AUDIT", auditSeverity(log)),
                humanize(log.getAction()),
                log.getSummary(),
                log.getCreatedAt(),
                null,
                null,
                purchaseOrderId,
                null,
                null
        );
    }

    private void addNotificationItems(List<AdminNotificationEntity> notifications, LocalDateTime start, LocalDateTime end, List<ExecutiveTimelineItemResponse> items) {
        notifications.stream()
                .filter(notification -> isToday(notification.getCreatedAt(), start, end))
                .limit(10)
                .map(notification -> new ExecutiveTimelineItemResponse(
                        "NOTIFICATION",
                        normalizeSeverity(notification.getSeverity()),
                        priorityScore("NOTIFICATION", normalizeSeverity(notification.getSeverity())),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getCreatedAt(),
                        notification.getId(),
                        null,
                        notification.getPurchaseOrderId(),
                        null,
                        notification.getSupplierId()
                ))
                .forEach(items::add);
    }

    private void addPurchaseOrderItems(List<PurchaseOrderEntity> purchaseOrders, LocalDateTime start, LocalDateTime end, List<ExecutiveTimelineItemResponse> items) {
        purchaseOrders.stream()
                .filter(order -> isToday(order.getCreatedAt(), start, end))
                .limit(10)
                .map(order -> new ExecutiveTimelineItemResponse(
                        "PURCHASE_ORDER",
                        orderSeverity(order),
                        priorityScore("PURCHASE_ORDER", orderSeverity(order)),
                        "Commande fournisseur #" + order.getId() + " - " + order.getStatus(),
                        order.getLines().size() + " ligne(s), fournisseur #" + order.getSupplierId(),
                        order.getCreatedAt(),
                        null,
                        null,
                        order.getId(),
                        null,
                        order.getSupplierId()
                ))
                .forEach(items::add);
    }

    private void addRiskItems(List<AiStockoutRiskEntity> risks, LocalDateTime start, LocalDateTime end, List<ExecutiveTimelineItemResponse> items) {
        risks.stream()
                .filter(this::isCriticalRisk)
                .filter(risk -> isToday(risk.getGeneratedAt(), start, end))
                .limit(5)
                .map(risk -> new ExecutiveTimelineItemResponse(
                        "AI_RISK",
                        "CRITICAL",
                        riskPriorityScore(risk),
                        "Risque de rupture produit #" + risk.getProductId(),
                        riskDescription(risk),
                        risk.getGeneratedAt(),
                        null,
                        risk.getProductId(),
                        null,
                        null,
                        null
                ))
                .forEach(items::add);
    }

    private void addRecommendationItems(List<AiReorderRecommendationEntity> recommendations, LocalDateTime start, LocalDateTime end, List<ExecutiveTimelineItemResponse> items) {
        recommendations.stream()
                .filter(recommendation -> recommendation.getPurchaseOrderId() == null)
                .filter(recommendation -> isToday(recommendation.getGeneratedAt(), start, end))
                .limit(5)
                .map(recommendation -> new ExecutiveTimelineItemResponse(
                        "AI_RECOMMENDATION",
                        "WARNING",
                        priorityScore("AI_RECOMMENDATION", "WARNING"),
                        "Reco commande produit #" + recommendation.getProductId(),
                        recommendation.getRecommendedQuantity() + " unite(s) recommandees. " + safe(recommendation.getReason()),
                        recommendation.getGeneratedAt(),
                        null,
                        recommendation.getProductId(),
                        null,
                        recommendation.getId(),
                        null
                ))
                .forEach(items::add);
    }

    private String executiveSummary(int criticalRisks, int openNotifications, int pendingOrders, int pendingRecommendations) {
        if (criticalRisks > 0) {
            return "Priorite du jour: securiser les ruptures critiques avant de traiter les optimisations.";
        }
        if (pendingOrders > 0) {
            return "Priorite du jour: faire avancer les commandes fournisseurs pour reduire le risque operationnel.";
        }
        if (pendingRecommendations > 0) {
            return "Priorite du jour: transformer les recommandations IA utiles en commandes actionnables.";
        }
        if (openNotifications > 0) {
            return "Priorite du jour: nettoyer les alertes admin ouvertes pour garder un cockpit fiable.";
        }
        return "La situation est stable aujourd'hui. Surveille le cockpit IA et les fournisseurs sensibles.";
    }

    private List<String> keyDecisions(int criticalRisks, int openNotifications, int pendingOrders, int pendingRecommendations) {
        List<String> decisions = new ArrayList<>();
        if (criticalRisks > 0) {
            decisions.add("Verifier les produits en rupture probable et commander ou ajuster les seuils.");
        }
        if (pendingOrders > 0) {
            decisions.add("Valider, envoyer ou receptionner les commandes fournisseurs en attente.");
        }
        if (pendingRecommendations > 0) {
            decisions.add("Convertir les recommandations IA avec fournisseur fiable en commandes DRAFT.");
        }
        if (openNotifications > 0) {
            decisions.add("Traiter les notifications critiques et fermer celles deja resolues.");
        }
        if (decisions.isEmpty()) {
            decisions.add("Continuer le suivi standard et verifier les tendances dans le dashboard IA.");
        }
        return decisions;
    }

    private List<ExecutiveTimelineActionResponse> actions(int criticalRisks, int openNotifications, int pendingOrders, int pendingRecommendations) {
        List<ExecutiveTimelineActionResponse> actions = new ArrayList<>();
        if (criticalRisks > 0) {
            actions.add(new ExecutiveTimelineActionResponse(
                    "CRITICAL",
                    "Traiter les ruptures critiques",
                    criticalRisks + " produit(s) demandent une decision immediate.",
                    "/stock-health"
            ));
        }
        if (pendingOrders > 0) {
            actions.add(new ExecutiveTimelineActionResponse(
                    "HIGH",
                    "Avancer les commandes fournisseurs",
                    pendingOrders + " commande(s) sont encore a valider, envoyer ou recevoir.",
                    "/approvals"
            ));
        }
        if (pendingRecommendations > 0) {
            actions.add(new ExecutiveTimelineActionResponse(
                    "MEDIUM",
                    "Convertir les recommandations IA",
                    pendingRecommendations + " recommandation(s) peuvent devenir des commandes.",
                    "/ai-dashboard"
            ));
        }
        if (openNotifications > 0) {
            actions.add(new ExecutiveTimelineActionResponse(
                    "MEDIUM",
                    "Lire les alertes admin",
                    openNotifications + " notification(s) ouvertes ou non lues.",
                    "/notifications"
            ));
        }
        if (actions.isEmpty()) {
            actions.add(new ExecutiveTimelineActionResponse(
                    "LOW",
                    "Surveiller le cockpit",
                    "Aucun blocage majeur detecte aujourd'hui.",
                    "/ai-dashboard"
            ));
        }
        return actions;
    }

    private boolean isToday(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        return dateTime != null && !dateTime.isBefore(start) && dateTime.isBefore(end);
    }

    private boolean isCriticalRisk(AiStockoutRiskEntity risk) {
        return "HIGH".equalsIgnoreCase(risk.getRiskLevel()) || risk.getRiskScore().compareTo(CRITICAL_RISK_SCORE) >= 0;
    }

    private boolean isPendingOrder(PurchaseOrderEntity order) {
        return order.getStatus() == PurchaseOrderStatus.DRAFT
                || order.getStatus() == PurchaseOrderStatus.APPROVED
                || order.getStatus() == PurchaseOrderStatus.ORDERED;
    }

    private String orderSeverity(PurchaseOrderEntity order) {
        return switch (order.getStatus()) {
            case DRAFT, APPROVED -> "WARNING";
            case ORDERED -> "INFO";
            case RECEIVED -> "SUCCESS";
            case CANCELLED -> "CRITICAL";
        };
    }

    private String auditSeverity(AiAuditLogEntity log) {
        String action = log.getAction().toUpperCase(Locale.ROOT);
        String summary = log.getSummary().toUpperCase(Locale.ROOT);
        if (action.contains("DELETE") || action.contains("CANCEL") || summary.contains("CRITICAL") || summary.contains("CRITIQUE")) {
            return "CRITICAL";
        }
        if (action.contains("UPDATE") || action.contains("APPROVE") || action.contains("IMPORT") || action.contains("THRESHOLD")) {
            return "WARNING";
        }
        return "INFO";
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "INFO";
        }
        return severity.toUpperCase(Locale.ROOT);
    }

    private String riskDescription(AiStockoutRiskEntity risk) {
        String date = risk.getEstimatedStockoutDate() == null ? "date inconnue" : risk.getEstimatedStockoutDate().toString();
        return "Score " + risk.getRiskScore() + ", rupture estimee " + date + ". " + safe(risk.getReason());
    }

    private String humanize(String action) {
        return action == null ? "Action" : action.replace('_', ' ');
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int priorityScore(String type, String severity) {
        int base = switch (normalizeSeverity(severity)) {
            case "CRITICAL" -> 90;
            case "WARNING" -> 70;
            case "SUCCESS" -> 25;
            default -> 45;
        };
        return base + switch (type) {
            case "AI_RISK" -> 8;
            case "AI_RECOMMENDATION" -> 6;
            case "PURCHASE_ORDER" -> 5;
            case "NOTIFICATION" -> 4;
            default -> 0;
        };
    }

    private int riskPriorityScore(AiStockoutRiskEntity risk) {
        int score = Math.min(100, risk.getRiskScore().intValue());
        return Math.max(priorityScore("AI_RISK", "CRITICAL"), score);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
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
}
