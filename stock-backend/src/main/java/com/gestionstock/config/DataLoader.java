package com.gestionstock.config;

import com.gestionstock.ai.infrastructure.entity.*;
import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.notification.domain.service.AdminNotificationService;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
import com.gestionstock.procurement.infrastructure.entity.ProductSupplierEntity;
import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderEntity;
import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderLineEntity;
import com.gestionstock.procurement.infrastructure.entity.SupplierEntity;
import com.gestionstock.procurement.infrastructure.repository.ProductSupplierJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.PurchaseOrderJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.SupplierJpaRepository;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.sales.domain.model.Sale;
import com.gestionstock.sales.domain.model.SaleLine;
import com.gestionstock.sales.domain.model.SaleStatus;
import com.gestionstock.sales.domain.model.SalesChannel;
import com.gestionstock.sales.domain.repository.SalesRepository;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.model.StockMovement;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "Password123!";

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplierJpaRepository supplierRepository;
    private final ProductSupplierJpaRepository productSupplierRepository;
    private final PurchaseOrderJpaRepository purchaseOrderRepository;
    private final SalesRepository salesRepository;
    private final AiForecastRepository forecastRepository;
    private final AiStockoutRiskRepository stockoutRiskRepository;
    private final AiReorderRecommendationRepository reorderRepository;
    private final AiAnomalyRepository anomalyRepository;
    private final AiInsightRepository insightRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final AdminNotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Value("${stockpilot.owner.enabled:false}")
    private boolean ownerEnabled;

    @Value("${stockpilot.owner.email:owner@stockpilot.local}")
    private String ownerEmail;

    @Value("${stockpilot.owner.password:}")
    private String ownerPassword;

    @Override
    public void run(String... args) {
        seedOwner();
        demoScenarios().forEach(this::seedScenario);
    }

    private void seedOwner() {
        if (!ownerEnabled || ownerPassword == null || ownerPassword.isBlank()) {
            return;
        }
        String normalizedEmail = ownerEmail.trim().toLowerCase();
        Organisation platform = organisationRepository.findByName("StockPilot Platform")
                .orElseGet(() -> organisationRepository.save(Organisation.builder()
                        .name("StockPilot Platform")
                        .industry("SaaS")
                        .sizeRange("1-10")
                        .city("Paris")
                        .country("France")
                        .currency("EUR")
                        .status("ACTIVE")
                        .onboardingCompleted(true)
                        .build()));
        userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(normalizedEmail)
                        .password(passwordEncoder.encode(ownerPassword))
                        .organisation(platform)
                        .role(Role.OWNER)
                        .build()));
    }

    private void seedScenario(DemoScenario scenario) {
        Organisation organisation = organisationRepository.findByName(scenario.organisationName())
                .orElseGet(() -> organisationRepository.save(Organisation.builder()
                        .name(scenario.organisationName())
                        .industry(scenario.industry())
                        .sizeRange(scenario.sizeRange())
                        .phone(scenario.phone())
                        .address(scenario.address())
                        .city(scenario.city())
                        .country(scenario.country())
                        .currency(scenario.currency())
                        .onboardingCompleted(true)
                        .build()));
        enrichOrganisation(organisation, scenario);
        seedSubscription(organisation, scenario.planCode());

        User admin = seedUser(scenario.adminEmail(), Role.ADMIN, organisation);
        seedUser(scenario.userEmail(), Role.USER, organisation);

        List<Product> products = scenario.products().stream()
                .map(product -> seedProduct(organisation.getId(), product))
                .toList();
        List<SupplierEntity> suppliers = seedSuppliers(organisation.getId(), scenario);
        seedProductSuppliers(organisation.getId(), products, suppliers);
        seedMovementHistory(organisation.getId(), products);
        seedSales(organisation.getId(), products);
        seedAiData(organisation.getId(), products);
        seedPurchaseOrders(organisation.getId(), products, suppliers);
        seedNotifications(organisation.getId(), products, suppliers);
    }

    private void enrichOrganisation(Organisation organisation, DemoScenario scenario) {
        boolean changed = false;
        if (!organisation.isOnboardingCompleted()) {
            organisation.setOnboardingCompleted(true);
            changed = true;
        }
        if (organisation.getIndustry() == null) {
            organisation.setIndustry(scenario.industry());
            organisation.setSizeRange(scenario.sizeRange());
            organisation.setPhone(scenario.phone());
            organisation.setAddress(scenario.address());
            organisation.setCity(scenario.city());
            organisation.setCountry(scenario.country());
            organisation.setCurrency(scenario.currency());
            changed = true;
        }
        if (changed) {
            organisationRepository.save(organisation);
        }
    }

    private void seedSubscription(Organisation organisation, String planCode) {
        billingSubscriptionRepository.findByOrganisationId(organisation.getId())
                .orElseGet(() -> {
                    BillingSubscriptionEntity subscription = new BillingSubscriptionEntity();
                    subscription.setOrganisation(organisation);
                    subscription.setPlanCode(planCode);
                    subscription.setStatus("TRIALING");
                    subscription.setTrialEndsAt(LocalDateTime.now().plusDays(14));
                    return billingSubscriptionRepository.save(subscription);
                });
    }

    private User seedUser(String email, Role role, Organisation organisation) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(DEMO_PASSWORD))
                        .organisation(organisation)
                        .role(role)
                        .build()));
    }

    private Product seedProduct(Long organisationId, DemoProduct demoProduct) {
        Product product = productRepository.findBySku(demoProduct.sku(), organisationId)
                .orElseGet(() -> productRepository.save(Product.builder()
                        .organisationId(organisationId)
                        .name(demoProduct.name())
                        .sku(demoProduct.sku())
                        .category(demoProduct.category())
                        .minStock(demoProduct.minStock())
                        .unit(demoProduct.unit())
                        .build()));
        stockRepository.findByProduct(product.id(), organisationId)
                .orElseGet(() -> stockRepository.save(Stock.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .quantity(demoProduct.quantity())
                        .build()));
        return product;
    }

    private List<SupplierEntity> seedSuppliers(Long organisationId, DemoScenario scenario) {
        return scenario.suppliers().stream()
                .map(supplier -> supplierRepository.findByOrganisationIdOrderByNameAsc(organisationId)
                        .stream()
                        .filter(existing -> existing.getName().equals(supplier.name()))
                        .findFirst()
                        .orElseGet(() -> supplierRepository.save(SupplierEntity.builder()
                                .organisationId(organisationId)
                                .name(supplier.name())
                                .email(supplier.email())
                                .phone(supplier.phone())
                                .leadTimeDays(supplier.leadTimeDays())
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .build())))
                .toList();
    }

    private void seedProductSuppliers(Long organisationId, List<Product> products, List<SupplierEntity> suppliers) {
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            SupplierEntity preferred = suppliers.get(index % suppliers.size());
            SupplierEntity alternative = suppliers.get((index + 1) % suppliers.size());
            seedProductSupplier(organisationId, product, preferred, true, index, BigDecimal.valueOf(8 + (index * 3L)));
            seedProductSupplier(organisationId, product, alternative, false, index, BigDecimal.valueOf(10 + (index * 2L)));
        }
    }

    private void seedProductSupplier(
            Long organisationId,
            Product product,
            SupplierEntity supplier,
            boolean preferred,
            int index,
            BigDecimal unitCost
    ) {
        productSupplierRepository.findByOrganisationIdAndProductIdAndSupplierId(organisationId, product.id(), supplier.getId())
                .orElseGet(() -> productSupplierRepository.save(ProductSupplierEntity.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .supplierId(supplier.getId())
                        .unitCost(unitCost)
                        .minimumOrderQuantity(Math.max(5, product.minStock() + (preferred ? 0 : 3)))
                        .preferred(preferred)
                        .active(true)
                        .createdAt(LocalDateTime.now().minusDays(index))
                        .build()));
    }

    private void seedMovementHistory(Long organisationId, List<Product> products) {
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            if (stockMovementRepository.countHistory(organisationId, product.id(), null) >= 6) {
                continue;
            }
            for (int month = 0; month < 6; month++) {
                stockMovementRepository.save(StockMovement.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .quantity(18 + index + (month * 3))
                        .type(MovementType.IN)
                        .createdAt(now.minusDays(38L - month * 6L + index))
                        .build());
                stockMovementRepository.save(StockMovement.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .quantity(Math.max(1, 5 + (index % 6) + month))
                        .type(MovementType.OUT)
                        .createdAt(now.minusDays(34L - month * 5L + index))
                        .build());
            }
            if (index % 4 == 0) {
                stockMovementRepository.save(StockMovement.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .quantity(3 + index)
                        .type(MovementType.ADJUST)
                        .createdAt(now.minusDays(2L + index))
                        .build());
            }
        }
    }

    private void seedSales(Long organisationId, List<Product> products) {
        if (!salesRepository.findBetween(organisationId, LocalDateTime.now().minusDays(90), LocalDateTime.now()).isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int day = 1; day <= 45; day += 3) {
            Product first = products.get(day % products.size());
            Product second = products.get((day + 3) % products.size());
            SalesChannel channel = switch (day % 4) {
                case 0 -> SalesChannel.WEB;
                case 1 -> SalesChannel.STORE;
                case 2 -> SalesChannel.PHONE;
                default -> SalesChannel.B2B;
            };
            LocalDateTime soldAt = now.minusDays(day).withHour(10 + day % 8).withMinute(15);
            SaleLine firstLine = demoSaleLine(organisationId, first, 2 + day % 4, BigDecimal.valueOf(18 + day + first.minStock()));
            SaleLine secondLine = demoSaleLine(organisationId, second, 1 + day % 3, BigDecimal.valueOf(12 + day + second.minStock()));
            salesRepository.save(Sale.builder()
                    .organisationId(organisationId)
                    .reference("DEMO-" + organisationId + "-" + day)
                    .customerName(day % 2 == 0 ? "Client comptoir" : "Compte pro " + day)
                    .channel(channel)
                    .status(SaleStatus.COMPLETED)
                    .totalAmount(firstLine.lineTotal().add(secondLine.lineTotal()))
                    .soldAt(soldAt)
                    .createdAt(soldAt)
                    .lines(List.of(firstLine, secondLine))
                    .build());
        }
    }

    private SaleLine demoSaleLine(Long organisationId, Product product, int quantity, BigDecimal unitPrice) {
        BigDecimal price = unitPrice.setScale(2);
        return SaleLine.builder()
                .organisationId(organisationId)
                .productId(product.id())
                .quantity(quantity)
                .unitPrice(price)
                .lineTotal(price.multiply(BigDecimal.valueOf(quantity)).setScale(2))
                .build();
    }

    private void seedAiData(Long organisationId, List<Product> products) {
        if (!forecastRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId).isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            forecastRepository.save(AiForecastEntity.builder()
                    .organisationId(organisationId)
                    .productId(product.id())
                    .horizonDays(7)
                    .predictedQuantity(BigDecimal.valueOf(14 + index))
                    .confidenceScore(BigDecimal.valueOf(0.86))
                    .modelName("demo-prophet")
                    .generatedAt(now.minusHours(index))
                    .build());
            forecastRepository.save(AiForecastEntity.builder()
                    .organisationId(organisationId)
                    .productId(product.id())
                    .horizonDays(30)
                    .predictedQuantity(BigDecimal.valueOf(42 + index * 3L))
                    .confidenceScore(BigDecimal.valueOf(0.81))
                    .modelName("demo-xgboost")
                    .generatedAt(now.minusHours(index))
                    .build());
            forecastRepository.save(AiForecastEntity.builder()
                    .organisationId(organisationId)
                    .productId(product.id())
                    .horizonDays(90)
                    .predictedQuantity(BigDecimal.valueOf(115 + index * 7L))
                    .confidenceScore(BigDecimal.valueOf(0.74))
                    .modelName("demo-ensemble")
                    .generatedAt(now.minusHours(index))
                    .build());
            if (index % 3 == 0) {
                stockoutRiskRepository.save(AiStockoutRiskEntity.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .estimatedStockoutDate(LocalDate.now().plusDays(2 + index))
                        .riskScore(BigDecimal.valueOf(92 - index))
                        .riskLevel("HIGH")
                        .reason("Demande acceleree et couverture stock insuffisante.")
                        .generatedAt(now.minusHours(index))
                        .build());
                reorderRepository.save(AiReorderRecommendationEntity.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .recommendedQuantity(30 + index * 2)
                        .leadTimeDays(3 + index % 5)
                        .safetyStock(8 + index % 4)
                        .reason("Recommande pour couvrir la demande prevue avec stock de securite.")
                        .status("PENDING")
                        .generatedAt(now.minusHours(index))
                        .build());
            }
            if (index % 5 == 0) {
                anomalyRepository.save(AiAnomalyEntity.builder()
                        .organisationId(organisationId)
                        .productId(product.id())
                        .anomalyType("UNUSUAL_OUT")
                        .severity(index % 10 == 0 ? "HIGH" : "MEDIUM")
                        .score(BigDecimal.valueOf(0.79 + (index % 2) * 0.1))
                        .explanation("Sortie stock superieure au profil historique du produit.")
                        .detectedAt(now.minusDays(index % 7))
                        .build());
            }
        }
        insightRepository.save(AiInsightEntity.builder()
                .organisationId(organisationId)
                .title("Priorite stock critique")
                .content("Plusieurs references approchent d'une rupture probable sous 7 jours. Les commandes recommandees sont pretes a etre validees.")
                .insightType("STOCK_HEALTH")
                .priority("HIGH")
                .generatedAt(now)
                .build());
        insightRepository.save(AiInsightEntity.builder()
                .organisationId(organisationId)
                .title("Optimisation fournisseurs")
                .content("Certains produits peuvent reduire leur lead time en basculant vers le fournisseur alternatif le mieux score.")
                .insightType("SUPPLIER")
                .priority("MEDIUM")
                .generatedAt(now.minusHours(2))
                .build());
    }

    private void seedPurchaseOrders(Long organisationId, List<Product> products, List<SupplierEntity> suppliers) {
        if (!purchaseOrderRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId).isEmpty()) {
            return;
        }
        for (int index = 0; index < Math.min(4, suppliers.size() + 1); index++) {
            SupplierEntity supplier = suppliers.get(index % suppliers.size());
            PurchaseOrderStatus status = switch (index) {
                case 0 -> PurchaseOrderStatus.DRAFT;
                case 1 -> PurchaseOrderStatus.ORDERED;
                case 2 -> PurchaseOrderStatus.RECEIVED;
                default -> PurchaseOrderStatus.APPROVED;
            };
            PurchaseOrderEntity order = PurchaseOrderEntity.builder()
                    .organisationId(organisationId)
                    .supplierId(supplier.getId())
                    .status(status)
                    .expectedDeliveryDate(LocalDate.now().plusDays(supplier.getLeadTimeDays() + index))
                    .createdAt(LocalDateTime.now().minusDays(10L - index))
                    .receivedAt(status == PurchaseOrderStatus.RECEIVED ? LocalDateTime.now().minusDays(1) : null)
                    .build();
            for (int line = 0; line < 2; line++) {
                Product product = products.get((index + line) % products.size());
                PurchaseOrderLineEntity orderLine = PurchaseOrderLineEntity.builder()
                        .purchaseOrder(order)
                        .productId(product.id())
                        .quantity(12 + index * 4 + line * 3)
                        .receivedQuantity(status == PurchaseOrderStatus.RECEIVED ? 12 + index * 4 + line * 3 : 0)
                        .unitCost(BigDecimal.valueOf(9 + index * 5L + line * 4L))
                        .build();
                order.getLines().add(orderLine);
            }
            purchaseOrderRepository.save(order);
        }
    }

    private void seedNotifications(Long organisationId, List<Product> products, List<SupplierEntity> suppliers) {
        if (products.isEmpty() || suppliers.isEmpty()) {
            return;
        }
        notificationService.createOnce(
                organisationId,
                "AI_CRITICAL_STOCKOUT",
                "CRITICAL",
                "Rupture critique detectee",
                "L'IA prevoit une rupture sous 5 jours sur une reference prioritaire.",
                null,
                suppliers.getFirst().getId(),
                "demo-critical-stockout"
        );
        notificationService.createOnce(
                organisationId,
                "PURCHASE_ORDER_THRESHOLD",
                "WARNING",
                "Commande fournisseur a valider",
                "Une commande depasse le seuil d'approbation et attend une decision ADMIN.",
                purchaseOrderRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId).stream()
                        .filter(order -> order.getStatus() == PurchaseOrderStatus.DRAFT || order.getStatus() == PurchaseOrderStatus.APPROVED)
                        .findFirst()
                        .map(PurchaseOrderEntity::getId)
                        .orElse(null),
                suppliers.getFirst().getId(),
                "demo-purchase-threshold"
        );
    }

    private List<DemoScenario> demoScenarios() {
        return List.of(
                new DemoScenario(
                        "Demo Stock",
                        "Retail",
                        "1-10",
                        "+33 1 23 45 67 89",
                        "12 rue des Inventaires",
                        "Paris",
                        "France",
                        "EUR",
                        "PRO",
                        "admin@demo-stock.local",
                        "user@demo-stock.local",
                        List.of(
                                new DemoSupplier("TechParts Europe", "orders@techparts.local", "+33 1 23 45 67 89", 4),
                                new DemoSupplier("PackSupply Express", "sales@packsupply.local", "+33 4 55 66 77 88", 2),
                                new DemoSupplier("Office Prime", "supply@office-prime.local", "+33 7 12 11 10 09", 5)
                        ),
                        List.of(
                                new DemoProduct("INV-LAP-001", "Laptop Pro 14", "Informatique", 8, "pcs", 24),
                                new DemoProduct("INV-MON-002", "Ecran 27 pouces", "Informatique", 10, "pcs", 7),
                                new DemoProduct("INV-KBD-003", "Clavier mecanique", "Accessoires", 12, "pcs", 46),
                                new DemoProduct("INV-MOU-004", "Souris sans fil", "Accessoires", 15, "pcs", 11),
                                new DemoProduct("INV-BOX-005", "Cartons expedition", "Logistique", 50, "pcs", 120),
                                new DemoProduct("INV-LBL-006", "Etiquettes code-barres", "Logistique", 80, "roll", 72),
                                new DemoProduct("INV-CAM-007", "Camera entrepot", "Securite", 4, "pcs", 3),
                                new DemoProduct("INV-BAT-008", "Batterie scanner", "Maintenance", 6, "pcs", 0),
                                new DemoProduct("INV-SCN-009", "Scanner code-barres", "Informatique", 6, "pcs", 9),
                                new DemoProduct("INV-DOC-010", "Rouleaux imprimante", "Logistique", 60, "roll", 94)
                        )
                ),
                new DemoScenario(
                        "Garage Atlas",
                        "Garage automobile",
                        "2-20",
                        "+33 3 44 55 66 77",
                        "8 avenue des Moteurs",
                        "Lyon",
                        "France",
                        "EUR",
                        "STARTER",
                        "admin@garage-atlas.local",
                        "mecano@garage-atlas.local",
                        List.of(
                                new DemoSupplier("AutoParts Nord", "commande@autoparts-nord.local", "+33 3 22 44 66 88", 3),
                                new DemoSupplier("Garage Supply Pro", "sales@garage-supply.local", "+33 6 44 22 11 09", 5),
                                new DemoSupplier("Pneus Express", "contact@pneus-express.local", "+33 5 88 77 66 55", 2)
                        ),
                        List.of(
                                new DemoProduct("GAR-OIL-001", "Huile moteur 5W30", "Lubrifiants", 20, "bidon", 65),
                                new DemoProduct("GAR-FLT-002", "Filtre a huile", "Filtres", 15, "pcs", 22),
                                new DemoProduct("GAR-AIR-003", "Filtre a air", "Filtres", 15, "pcs", 18),
                                new DemoProduct("GAR-BRK-004", "Plaquettes frein avant", "Freinage", 10, "set", 8),
                                new DemoProduct("GAR-BAT-005", "Batterie 12V", "Electricite", 5, "pcs", 4),
                                new DemoProduct("GAR-TYR-006", "Pneu ete 205/55", "Pneumatiques", 12, "pcs", 31),
                                new DemoProduct("GAR-SPK-007", "Bougies allumage", "Moteur", 18, "pcs", 40),
                                new DemoProduct("GAR-WIP-008", "Balais essuie-glace", "Accessoires", 14, "pair", 19),
                                new DemoProduct("GAR-CLN-009", "Nettoyant frein", "Consommables", 24, "spray", 73),
                                new DemoProduct("GAR-LMP-010", "Ampoule H7", "Electricite", 16, "pcs", 12)
                        )
                ),
                new DemoScenario(
                        "Pharma Nova",
                        "Pharmacie",
                        "5-30",
                        "+33 2 18 45 90 12",
                        "22 place Sante",
                        "Nantes",
                        "France",
                        "EUR",
                        "PRO",
                        "admin@pharma-nova.local",
                        "preparateur@pharma-nova.local",
                        List.of(
                                new DemoSupplier("MediLog France", "orders@medilog.local", "+33 2 22 33 44 55", 1),
                                new DemoSupplier("Pharma Grossiste", "commande@pharma-grossiste.local", "+33 1 88 90 44 31", 2),
                                new DemoSupplier("Care Packaging", "hello@care-packaging.local", "+33 4 90 12 13 14", 4)
                        ),
                        List.of(
                                new DemoProduct("PHA-MSK-001", "Masques chirurgicaux", "Protection", 120, "box", 280),
                                new DemoProduct("PHA-GEL-002", "Gel hydroalcoolique 500ml", "Hygiene", 40, "bottle", 76),
                                new DemoProduct("PHA-PAR-003", "Paracetamol 500mg", "Medicament", 80, "box", 160),
                                new DemoProduct("PHA-VIT-004", "Vitamine C", "Complement", 35, "box", 24),
                                new DemoProduct("PHA-TST-005", "Tests rapides", "Diagnostic", 30, "box", 18),
                                new DemoProduct("PHA-BND-006", "Bandes elastiques", "Soins", 25, "pcs", 52),
                                new DemoProduct("PHA-CRM-007", "Creme reparatrice", "Dermatologie", 20, "tube", 15),
                                new DemoProduct("PHA-SYR-008", "Seringues sterile", "Materiel", 60, "box", 91),
                                new DemoProduct("PHA-GLV-009", "Gants nitrile", "Protection", 90, "box", 138),
                                new DemoProduct("PHA-THM-010", "Thermometres digitaux", "Diagnostic", 12, "pcs", 6)
                        )
                ),
                new DemoScenario(
                        "Boutique Lumiere",
                        "Boutique deco",
                        "1-15",
                        "+33 7 91 10 11 12",
                        "4 rue des Vitrines",
                        "Bordeaux",
                        "France",
                        "EUR",
                        "STARTER",
                        "admin@boutique-lumiere.local",
                        "vendeur@boutique-lumiere.local",
                        List.of(
                                new DemoSupplier("Maison Deco", "pro@maison-deco.local", "+33 5 56 12 45 78", 6),
                                new DemoSupplier("Lumina Wholesale", "orders@lumina.local", "+33 5 42 13 14 15", 4),
                                new DemoSupplier("Green Candles", "sales@green-candles.local", "+33 6 11 22 33 44", 5)
                        ),
                        List.of(
                                new DemoProduct("BTQ-CND-001", "Bougie parfumee", "Bougies", 20, "pcs", 45),
                                new DemoProduct("BTQ-VAS-002", "Vase ceramique", "Decoration", 8, "pcs", 16),
                                new DemoProduct("BTQ-LMP-003", "Lampe table", "Luminaire", 6, "pcs", 7),
                                new DemoProduct("BTQ-PLA-004", "Plaid coton", "Textile", 10, "pcs", 21),
                                new DemoProduct("BTQ-MIR-005", "Miroir rond", "Decoration", 5, "pcs", 3),
                                new DemoProduct("BTQ-FRM-006", "Cadre photo", "Decoration", 14, "pcs", 38),
                                new DemoProduct("BTQ-RUG-007", "Tapis entree", "Textile", 7, "pcs", 9),
                                new DemoProduct("BTQ-DIF-008", "Diffuseur huiles", "Bien-etre", 12, "pcs", 11),
                                new DemoProduct("BTQ-POT-009", "Cache-pot metal", "Decoration", 15, "pcs", 26),
                                new DemoProduct("BTQ-LED-010", "Guirlande LED", "Luminaire", 18, "pcs", 29)
                        )
                ),
                new DemoScenario(
                        "Atelier Meca",
                        "Petit fabricant",
                        "10-50",
                        "+33 4 72 80 11 22",
                        "31 zone industrielle Est",
                        "Grenoble",
                        "France",
                        "EUR",
                        "PRO",
                        "admin@atelier-meca.local",
                        "atelier@atelier-meca.local",
                        List.of(
                                new DemoSupplier("Metal Source", "orders@metal-source.local", "+33 4 77 88 99 00", 7),
                                new DemoSupplier("Fasteners Pro", "contact@fasteners-pro.local", "+33 3 90 12 11 10", 3),
                                new DemoSupplier("Industrial Tools", "sales@industrial-tools.local", "+33 1 45 99 77 33", 5)
                        ),
                        List.of(
                                new DemoProduct("MEC-STL-001", "Plaque acier 2mm", "Matiere premiere", 30, "sheet", 75),
                                new DemoProduct("MEC-ALU-002", "Profil aluminium", "Matiere premiere", 25, "bar", 52),
                                new DemoProduct("MEC-SCR-003", "Vis M6 inox", "Fixation", 200, "pcs", 480),
                                new DemoProduct("MEC-NUT-004", "Ecrous M6", "Fixation", 200, "pcs", 390),
                                new DemoProduct("MEC-BRG-005", "Roulements 608", "Composants", 40, "pcs", 33),
                                new DemoProduct("MEC-PNT-006", "Peinture epoxy", "Finition", 16, "kg", 19),
                                new DemoProduct("MEC-GLV-007", "Gants atelier", "Securite", 30, "pair", 41),
                                new DemoProduct("MEC-DSK-008", "Disque decoupe", "Outillage", 50, "pcs", 58),
                                new DemoProduct("MEC-MTR-009", "Micro moteur", "Composants", 12, "pcs", 8),
                                new DemoProduct("MEC-CBL-010", "Cable gaine", "Electricite", 60, "m", 95)
                        )
                )
        );
    }

    private record DemoScenario(
            String organisationName,
            String industry,
            String sizeRange,
            String phone,
            String address,
            String city,
            String country,
            String currency,
            String planCode,
            String adminEmail,
            String userEmail,
            List<DemoSupplier> suppliers,
            List<DemoProduct> products
    ) {
    }

    private record DemoSupplier(String name, String email, String phone, int leadTimeDays) {
    }

    private record DemoProduct(
            String sku,
            String name,
            String category,
            int minStock,
            String unit,
            int quantity
    ) {
    }
}
