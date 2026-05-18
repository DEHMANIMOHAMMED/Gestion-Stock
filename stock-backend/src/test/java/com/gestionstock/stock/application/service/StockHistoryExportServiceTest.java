package com.gestionstock.stock.application.service;

import com.gestionstock.stock.application.dto.StockMovementHistoryResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockHistoryExportServiceTest {

    private final StockHistoryExportService exportService = new StockHistoryExportService();

    @Test
    void toCsvExportsHeaderAndRows() {
        byte[] bytes = exportService.toCsv(List.of(new StockMovementHistoryResponse(
                1L,
                10L,
                5,
                "IN",
                LocalDateTime.of(2026, 5, 14, 10, 0)
        )));

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFid,productId,quantity,type,createdAt");
        assertThat(csv).contains("1,10,5,IN,2026-05-14T10:00");
    }

    @Test
    void toPdfExportsPdfBytes() throws Exception {
        byte[] bytes = exportService.toPdf(List.of(new StockMovementHistoryResponse(
                1L,
                10L,
                5,
                "OUT",
                LocalDateTime.of(2026, 5, 14, 11, 0)
        )));

        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
