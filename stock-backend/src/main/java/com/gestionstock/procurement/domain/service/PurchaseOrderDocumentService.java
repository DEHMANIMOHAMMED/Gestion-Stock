package com.gestionstock.procurement.domain.service;

import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderLine;
import com.gestionstock.procurement.domain.repository.PurchaseOrderRepository;
import com.gestionstock.procurement.domain.repository.SupplierRepository;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderDocumentService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public byte[] purchaseOrderPdf(Long id) {
        Long organisationId = TenantContext.requireOrganisationId();
        PurchaseOrder order = orderRepository.findById(id, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        var supplier = supplierRepository.findById(order.supplierId(), organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        Map<Long, Product> products = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                y = text(content, "Bon de commande #" + order.id(), 50, y, PDType1Font.HELVETICA_BOLD, 20);
                y = text(content, "Fournisseur: " + supplier.name(), 50, y - 14, PDType1Font.HELVETICA, 11);
                y = text(content, "Statut: " + order.status(), 50, y - 4, PDType1Font.HELVETICA, 11);
                y = text(content, "Livraison prevue: " + value(order.expectedDeliveryDate()), 50, y - 4, PDType1Font.HELVETICA, 11);
                y -= 22;
                y = text(content, "Produit", 50, y, PDType1Font.HELVETICA_BOLD, 10);
                text(content, "Qt.", 315, y + 14, PDType1Font.HELVETICA_BOLD, 10);
                text(content, "Recu", 360, y + 14, PDType1Font.HELVETICA_BOLD, 10);
                text(content, "Cout", 410, y + 14, PDType1Font.HELVETICA_BOLD, 10);
                text(content, "Total", 475, y + 14, PDType1Font.HELVETICA_BOLD, 10);
                y -= 6;
                for (PurchaseOrderLine line : order.lines()) {
                    Product product = products.get(line.productId());
                    BigDecimal total = lineTotal(line);
                    y = text(content, safe(product == null ? "Produit supprime" : product.name()), 50, y, PDType1Font.HELVETICA, 9);
                    text(content, String.valueOf(line.quantity()), 315, y + 13, PDType1Font.HELVETICA, 9);
                    text(content, String.valueOf(line.receivedQuantity()), 360, y + 13, PDType1Font.HELVETICA, 9);
                    text(content, money(line.unitCost()), 410, y + 13, PDType1Font.HELVETICA, 9);
                    text(content, money(total), 475, y + 13, PDType1Font.HELVETICA, 9);
                    y -= 2;
                }
                text(content, "Total commande: " + money(orderTotal(order)), 390, y - 18, PDType1Font.HELVETICA_BOLD, 11);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Purchase order PDF generation failed", exception);
        }
    }

    public byte[] accountingExportCsv() {
        Long organisationId = TenantContext.requireOrganisationId();
        Map<Long, Product> products = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        var suppliers = supplierRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(supplier -> supplier.id(), Function.identity()));

        StringBuilder csv = new StringBuilder("order_id,status,supplier,product,sku,quantity,received_quantity,unit_cost,total,created_at,received_at\n");
        orderRepository.findAll(organisationId).forEach(order -> order.lines().forEach(line -> {
            Product product = products.get(line.productId());
            var supplier = suppliers.get(order.supplierId());
            csv.append(order.id()).append(',')
                    .append(order.status()).append(',')
                    .append(escape(supplier == null ? "Supplier deleted" : supplier.name())).append(',')
                    .append(escape(product == null ? "Product deleted" : product.name())).append(',')
                    .append(escape(product == null ? "-" : product.sku())).append(',')
                    .append(line.quantity()).append(',')
                    .append(line.receivedQuantity()).append(',')
                    .append(money(line.unitCost())).append(',')
                    .append(money(lineTotal(line))).append(',')
                    .append(order.createdAt()).append(',')
                    .append(value(order.receivedAt())).append('\n');
        }));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private float text(PDPageContentStream content, String text, float x, float y, PDType1Font font, int size) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();
        return y - size - 4;
    }

    private BigDecimal orderTotal(PurchaseOrder order) {
        return order.lines().stream().map(this::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal lineTotal(PurchaseOrderLine line) {
        if (line.unitCost() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return line.unitCost().multiply(BigDecimal.valueOf(line.quantity())).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String escape(String value) {
        return "\"" + safe(value).replace("\"", "\"\"") + "\"";
    }

    private String safe(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
