package com.gestionstock.product.domain.service;

import com.gestionstock.audit.AuditLogService;
import com.gestionstock.product.application.dto.ProductImportResponse;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.domain.service.StockService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final ProductRepository productRepository;
    private final StockService stockService;
    private final AuthenticatedUserProvider userProvider;
    private final AuditLogService auditLogService;

    @Transactional
    public ProductImportResponse importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Import file is required");
        }
        List<ImportRow> rows = readRows(file);
        Long organisationId = TenantContext.requireOrganisationId();
        ImportCounters counters = new ImportCounters();
        List<String> errors = new ArrayList<>();

        for (ImportRow row : rows) {
            try {
                importRow(row, organisationId, counters);
            } catch (RuntimeException exception) {
                errors.add("Ligne " + row.lineNumber() + ": " + exception.getMessage());
            }
        }

        auditLogService.record(userProvider.requireUser(), "IMPORT_PRODUCTS", "PRODUCT", null, "CSV_EXCEL",
                counters.createdProducts + " created, " + counters.updatedProducts + " updated, "
                        + counters.stockMovementsCreated + " stock movements, " + errors.size() + " errors");
        return new ProductImportResponse(
                counters.createdProducts,
                counters.updatedProducts,
                counters.stockMovementsCreated,
                errors
        );
    }

    private void importRow(ImportRow row, Long organisationId, ImportCounters counters) {
        String sku = required(row.sku(), "SKU").toUpperCase();
        String name = required(row.name(), "Name");
        int minStock = parsePositiveInt(row.minStock(), "minStock", true);
        int initialStock = parsePositiveInt(row.initialStock(), "initialStock", true);

        Product imported = Product.builder()
                .organisationId(organisationId)
                .name(name)
                .sku(sku)
                .category(blankToNull(row.category()))
                .minStock(minStock)
                .unit(blankToNull(row.unit()))
                .build();

        var existingProduct = productRepository.findBySku(sku, organisationId);
        Product saved = existingProduct
                .map(existing -> productRepository.save(Product.builder()
                        .id(existing.id())
                        .organisationId(organisationId)
                        .name(imported.name())
                        .sku(sku)
                        .category(imported.category())
                        .minStock(imported.minStock())
                        .unit(imported.unit())
                .build()))
                .orElseGet(() -> productRepository.save(imported));

        if (saved.id() == null) {
            throw new IllegalArgumentException("Product import failed");
        }
        if (existingProduct.isPresent()) {
            counters.updatedProducts++;
        } else {
            counters.createdProducts++;
        }
        if (initialStock > 0) {
            stockService.registerMovement(saved.id(), initialStock, "ADJUST");
            counters.stockMovementsCreated++;
        }
    }

    private List<ImportRow> readRows(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return readExcelRows(file);
            }
            return readCsvRows(file);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to read import file: " + exception.getMessage());
        }
    }

    private List<ImportRow> readCsvRows(MultipartFile file) throws Exception {
        List<ImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.toLowerCase().contains("sku")) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split("[;,]", -1);
                rows.add(toRow(values, lineNumber));
            }
        }
        return rows;
    }

    private List<ImportRow> readExcelRows(MultipartFile file) throws Exception {
        List<ImportRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (var workbook = WorkbookFactory.create(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                int lineNumber = row.getRowNum() + 1;
                String[] values = new String[6];
                for (int i = 0; i < values.length; i++) {
                    values[i] = formatter.formatCellValue(row.getCell(i));
                }
                if (lineNumber == 1 && values[0].toLowerCase().contains("sku")) {
                    continue;
                }
                if (String.join("", values).isBlank()) {
                    continue;
                }
                rows.add(toRow(values, lineNumber));
            }
        }
        return rows;
    }

    private ImportRow toRow(String[] values, int lineNumber) {
        if (values.length < 6) {
            throw new IllegalArgumentException("Expected columns: sku,name,category,minStock,unit,initialStock");
        }
        return new ImportRow(
                lineNumber,
                values[0],
                values[1],
                values[2],
                values[3],
                values[4],
                values[5]
        );
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private int parsePositiveInt(String value, String field, boolean allowZero) {
        try {
            int parsed = Integer.parseInt(required(value, field));
            if (parsed < 0 || (!allowZero && parsed == 0)) {
                throw new IllegalArgumentException(field + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a number");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ImportRow(
            int lineNumber,
            String sku,
            String name,
            String category,
            String minStock,
            String unit,
            String initialStock
    ) {
    }

    private static final class ImportCounters {
        int createdProducts;
        int updatedProducts;
        int stockMovementsCreated;
    }
}
