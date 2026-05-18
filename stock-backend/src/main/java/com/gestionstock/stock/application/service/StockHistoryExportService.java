package com.gestionstock.stock.application.service;

import com.gestionstock.stock.application.dto.StockMovementHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockHistoryExportService {

    private static final String CSV_HEADER = "id,productId,quantity,type,createdAt";

    public byte[] toCsv(List<StockMovementHistoryResponse> movements) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append(CSV_HEADER).append(System.lineSeparator());

        for (var movement : movements) {
            csv.append(movement.id()).append(',')
                    .append(movement.productId()).append(',')
                    .append(movement.quantity()).append(',')
                    .append(movement.type()).append(',')
                    .append(movement.createdAt())
                    .append(System.lineSeparator());
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toPdf(List<StockMovementHistoryResponse> movements) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream content = null;
            try {
                content = new PDPageContentStream(document, page);
                content.setFont(PDType1Font.HELVETICA, 12);

                float y = 750f;
                y = writeLine(content, "Historique des mouvements de stock", y, 30);
                y = writeLine(content, "ID | Product | Qty | Type | Date", y, 20);

                for (var movement : movements) {
                    if (y < 40) {
                        content.close();
                        page = new PDPage();
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        content.setFont(PDType1Font.HELVETICA, 12);
                        y = writeLine(content, "ID | Product | Qty | Type | Date", 750f, 20);
                    }

                    String line = movement.id() + " | "
                            + movement.productId() + " | "
                            + movement.quantity() + " | "
                            + movement.type() + " | "
                            + movement.createdAt();
                    y = writeLine(content, line, y, 20);
                }
            } finally {
                if (content != null) {
                    content.close();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private float writeLine(PDPageContentStream content, String text, float y, float lineHeight) throws IOException {
        content.beginText();
        content.newLineAtOffset(50, y);
        content.showText(text);
        content.endText();
        return y - lineHeight;
    }
}
