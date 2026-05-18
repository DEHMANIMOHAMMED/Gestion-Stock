package com.gestionstock.procurement.domain.service;

import com.gestionstock.audit.AuditLogService;
import com.gestionstock.procurement.application.dto.ProcurementImportResponse;
import com.gestionstock.procurement.domain.model.PurchaseOrder;
import com.gestionstock.procurement.domain.model.PurchaseOrderLine;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcurementImportService {

    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;
    private final ProductRepository productRepository;
    private final AuthenticatedUserProvider userProvider;
    private final AuditLogService auditLogService;

    public ProcurementImportResponse importSuppliers(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        for (CsvRow row : rows(file, errors)) {
            try {
                supplierService.create(Supplier.builder()
                        .name(row.value("name"))
                        .email(blankToNull(row.value("email")))
                        .phone(blankToNull(row.value("phone")))
                        .leadTimeDays(parseInt(row.value("leadTimeDays"), 7))
                        .build());
                created++;
            } catch (RuntimeException exception) {
                skipped++;
                errors.add("Line " + row.lineNumber() + ": " + exception.getMessage());
            }
        }
        auditLogService.record(userProvider.requireUser(), "IMPORT_SUPPLIERS", "SUPPLIER", null, "CSV",
                created + " created, " + skipped + " skipped");
        return new ProcurementImportResponse(created, skipped, errors);
    }

    public ProcurementImportResponse importPurchaseOrders(MultipartFile file) {
        Long organisationId = TenantContext.requireOrganisationId();
        Map<String, Supplier> suppliers = supplierService.findAll().stream()
                .collect(Collectors.toMap(supplier -> supplier.name().toLowerCase(Locale.ROOT), Function.identity(), (left, ignored) -> left));
        List<String> errors = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        for (CsvRow row : rows(file, errors)) {
            try {
                Supplier supplier = suppliers.get(row.value("supplierName").toLowerCase(Locale.ROOT));
                if (supplier == null) {
                    throw new IllegalArgumentException("Supplier not found: " + row.value("supplierName"));
                }
                var product = productRepository.findBySku(row.value("sku"), organisationId)
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + row.value("sku")));
                purchaseOrderService.create(PurchaseOrder.builder()
                        .supplierId(supplier.id())
                        .expectedDeliveryDate(parseDate(row.value("expectedDeliveryDate")))
                        .lines(List.of(PurchaseOrderLine.builder()
                                .productId(product.id())
                                .quantity(parseInt(row.value("quantity"), 1))
                                .unitCost(parseDecimal(row.value("unitCost")))
                                .build()))
                        .build());
                created++;
            } catch (RuntimeException exception) {
                skipped++;
                errors.add("Line " + row.lineNumber() + ": " + exception.getMessage());
            }
        }
        auditLogService.record(userProvider.requireUser(), "IMPORT_PURCHASE_ORDERS", "PURCHASE_ORDER", null, "CSV",
                created + " created, " + skipped + " skipped");
        return new ProcurementImportResponse(created, skipped, errors);
    }

    private List<CsvRow> rows(MultipartFile file, List<String> errors) {
        List<CsvRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                errors.add("File is empty");
                return rows;
            }
            String[] headers = split(headerLine);
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String[] cells = split(line);
                rows.add(new CsvRow(lineNumber, headers, cells));
            }
        } catch (Exception exception) {
            errors.add("Import failed: " + exception.getMessage());
        }
        return rows;
    }

    private String[] split(String line) {
        return line.split(",", -1);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int parseInt(String value, int fallback) {
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }

    private BigDecimal parseDecimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }

    private record CsvRow(int lineNumber, String[] headers, String[] cells) {
        String value(String name) {
            for (int index = 0; index < headers.length; index++) {
                if (headers[index].trim().equalsIgnoreCase(name)) {
                    return index < cells.length ? cells[index].trim() : "";
                }
            }
            return "";
        }
    }
}
