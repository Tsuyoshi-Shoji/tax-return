package org.example.features.taxreturn.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class TaxReturnPdfRenderer {
    /** 計算・帳票レイアウトを変えたら更新する（2026-11-12.2: 決算書欄番号を令和5年分以降用様式の㉞〜㊺へ整合）。 */
    public static final String CALCULATION_RULE_VERSION = "2026-11-12.2";
    private static final Semaphore PDF_RENDER_PERMITS = new Semaphore(2);
    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'JST'").withZone(ZoneId.of("Asia/Tokyo"));

    /** 帳票フッターに記録する生成時点・様式版・締め状態・会計データ指紋（短縮）。 */
    public record OutputStamp(Instant generatedAt, String formEdition, String stateLabel, String shortFingerprint) {
    }

    public byte[] render(String html, String group, int year, String title)
            throws IOException, DocumentException {
        return render(html, group, year, title, null);
    }

    public byte[] render(String html, String group, int year, String title, OutputStamp stamp)
            throws IOException, DocumentException {
        if (!PDF_RENDER_PERMITS.tryAcquire()) {
            throw new PdfCapacityExceededException();
        }
        try {
            return renderWithinPermit(html, group, year, title, stamp);
        } finally {
            PDF_RENDER_PERMITS.release();
        }
    }

    private byte[] renderWithinPermit(String html, String group, int year, String title, OutputStamp stamp)
            throws IOException, DocumentException {
        org.jsoup.nodes.Document source = Jsoup.parse(html);
        List<Element> selectedSections = new ArrayList<>();
        for (Element section : source.select(".print-section")) {
            String[] groups = section.attr("data-print-groups").split("\\s+");
            for (String sectionGroup : groups) {
                if (group.equals(sectionGroup)) {
                    selectedSections.add(section);
                    break;
                }
            }
        }
        if (selectedSections.isEmpty()) {
            throw new PdfContentUnavailableException();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        com.lowagie.text.Document pdf = new com.lowagie.text.Document(PageSize.A4.rotate(), 24, 24, 24, 28);
        PdfWriter writer = PdfWriter.getInstance(pdf, output);
        writer.setPdfVersion(PdfWriter.PDF_VERSION_1_7);
        pdf.addTitle(title + " " + year + "年分");
        pdf.open();

        BaseFont japanese = BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font normal = new Font(japanese, 8);
        Font bold = new Font(japanese, 8, Font.BOLD);
        Font heading = new Font(japanese, 13, Font.BOLD);
        for (int sectionIndex = 0; sectionIndex < selectedSections.size(); sectionIndex++) {
            if (sectionIndex > 0) {
                pdf.newPage();
            }
            Element section = selectedSections.get(sectionIndex);
            for (Element child : section.children()) {
                appendElement(pdf, child, normal, bold, heading, japanese);
            }
        }

        Paragraph footer = new Paragraph(
                "対象年分: " + year + "年分　生成日時: "
                        + GENERATED_AT_FORMAT.format(stamp == null ? Instant.now() : stamp.generatedAt())
                        + "　計算ルール版: " + CALCULATION_RULE_VERSION
                        + (stamp == null ? "" : "　様式参照版: " + stamp.formEdition() + "　年度状態: "
                                + stamp.stateLabel() + "　データ指紋: " + stamp.shortFingerprint())
                        + "　集計値は画面と同一の年度処理によるものです。"
                        + "　本帳票は転記・確認用であり、国税庁の公式様式・提出データではありません。",
                new Font(japanese, 7));
        footer.setSpacingBefore(10);
        pdf.add(footer);
        pdf.close();
        return output.toByteArray();
    }

    private void appendElement(com.lowagie.text.Document pdf, Element element, Font normal, Font bold, Font heading,
            BaseFont japanese) throws DocumentException {
        switch (element.tagName()) {
            case "h1" -> pdf.add(new Paragraph(element.text(), heading));
            case "h2" -> pdf.add(new Paragraph(element.text(), heading));
            case "h3" -> {
                Paragraph paragraph = new Paragraph(element.text(), bold);
                paragraph.setSpacingBefore(4);
                pdf.add(paragraph);
            }
            case "p" -> {
                Paragraph paragraph = new Paragraph(element.text(), normal);
                paragraph.setSpacingAfter(4);
                pdf.add(paragraph);
            }
            case "table" -> appendTable(pdf, element, normal, bold, japanese);
            case "br" -> pdf.add(new Paragraph(" ", normal));
            default -> {
                String ownText = element.ownText().trim();
                if (!ownText.isEmpty()) {
                    pdf.add(new Paragraph(ownText, normal));
                }
                if (element.hasClass("tr-sheet-page") && element.previousElementSibling() != null) {
                    pdf.newPage();
                }
                for (Element child : element.children()) {
                    appendElement(pdf, child, normal, bold, heading, japanese);
                }
            }
        }
    }

    private void appendTable(com.lowagie.text.Document pdf, Element table, Font normal, Font bold, BaseFont japanese)
            throws DocumentException {
        Element caption = table.selectFirst("caption");
        if (caption != null) {
            Paragraph paragraph = new Paragraph(caption.text(), bold);
            paragraph.setSpacingBefore(3);
            pdf.add(paragraph);
        }

        List<Element> rows = table.select("tr");
        int columnCount = rows.stream().mapToInt(this::columnCount).max().orElse(1);
        PdfPTable output = new PdfPTable(columnCount);
        output.setWidthPercentage(100);
        output.setSpacingAfter(5);
        for (Element row : rows) {
            boolean header = "thead".equals(row.parent().tagName());
            int rowColumns = 0;
            for (Element cell : row.children()) {
                if (!"th".equals(cell.tagName()) && !"td".equals(cell.tagName())) {
                    continue;
                }
                Font cellFont = "th".equals(cell.tagName()) || header ? bold : normal;
                PdfPCell outputCell = new PdfPCell(new Phrase(cell.text(), cellFont));
                int cellColumns = colspan(cell);
                outputCell.setColspan(cellColumns);
                rowColumns += cellColumns;
                outputCell.setPadding(3);
                outputCell.setBorderColor(new Color(154, 168, 190));
                if (header) {
                    outputCell.setBackgroundColor(new Color(238, 243, 250));
                }
                if (cell.hasClass("num")) {
                    outputCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                }
                output.addCell(outputCell);
            }
            for (int i = rowColumns; i < columnCount; i++) {
                output.addCell(new PdfPCell(new Phrase("", normal)));
            }
        }
        pdf.add(output);
    }

    private int columnCount(Element row) {
        return row.children().stream()
                .filter(cell -> "th".equals(cell.tagName()) || "td".equals(cell.tagName()))
                .mapToInt(this::colspan)
                .sum();
    }

    private int colspan(Element cell) {
        try {
            return Math.max(1, Integer.parseInt(cell.attr("colspan")));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    public static final class PdfCapacityExceededException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static final class PdfContentUnavailableException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
