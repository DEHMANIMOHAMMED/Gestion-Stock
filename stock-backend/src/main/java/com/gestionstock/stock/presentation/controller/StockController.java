package com.gestionstock.stock.presentation.controller;

import com.gestionstock.stock.application.dto.StockMovementHistoryPageResponse;
import com.gestionstock.stock.application.dto.StockMovementRequest;
import com.gestionstock.stock.application.dto.StockResponse;
import com.gestionstock.stock.application.mapper.StockMapper;
import com.gestionstock.stock.application.service.StockHistoryExportService;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockMapper stockMapper;
    private final StockHistoryExportService exportService;

    @PostMapping("/movement")
    public ResponseEntity<Void> registerMovement(
            @Valid @RequestBody StockMovementRequest request) {

        stockService.registerMovement(
                request.productId(),
                request.quantity(),
                request.type()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long productId) {
        Stock stock = stockService.getStockForProduct(productId);
        return ResponseEntity.ok(stockMapper.toResponse(stock));
    }

    @GetMapping("/history")
    public ResponseEntity<StockMovementHistoryPageResponse> getHistory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                stockService.getHistory(productId, type, page, size)
        );
    }

    @GetMapping(value = "/history/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type
    ) {
        var allData = stockService.getAllHistory(productId, type);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=stock_history.csv")
                .body(exportService.toCsv(allData));
    }

    @GetMapping(value = "/history/export/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type
    ) throws IOException {

        var allData = stockService.getAllHistory(productId, type);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=stock_history.pdf")
                .body(exportService.toPdf(allData));
    }
}
