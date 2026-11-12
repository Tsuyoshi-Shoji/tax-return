package org.example.integration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.lowagie.text.pdf.PdfReader;
import org.example.features.taxreturn.export.TaxReturnPdfRenderer;
import org.example.features.taxreturn.export.TaxReturnPdfRenderer.PdfContentUnavailableException;

public final class TaxReturnPdfRendererHarness {
    private TaxReturnPdfRendererHarness() {
    }

    public static void main(String[] args) throws Exception {
        TaxReturnPdfRenderer renderer = new TaxReturnPdfRenderer();
        String html = """
                <!doctype html>
                <html><body>
                  <section class="print-section" data-print-groups="journal">
                    <h1>JOURNAL ONLY</h1>
                    <table><thead><tr><th>記録</th></tr></thead><tbody><tr><td>BOOK ROW</td></tr></tbody></table>
                  </section>
                  <section class="print-section" data-print-groups="ledger"><h1>LEDGER ONLY</h1></section>
                </body></html>
                """;
        byte[] pdf = renderer.render(html, "journal", 2026, "仕訳帳");
        require(pdf.length > 4 && pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F',
                "サーバー生成PDFのヘッダーがありません");

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf))) {
            byte[] content = reader.getPageContent(1);
            require(contains(content, utf16("JOURNAL ONLY")), "要求した帳票がPDFに含まれていません");
            require(contains(content, utf16("BOOK ROW")), "帳票の表データがPDFに含まれていません");
            require(!contains(content, utf16("LEDGER ONLY")), "別帳票の内容がPDFに混入しています");
            require(contains(content, utf16("2026")), "対象年分がPDFにありません");
            require(contains(content, utf16(TaxReturnPdfRenderer.CALCULATION_RULE_VERSION)),
                    "計算ルール版がPDFにありません");
        }

        byte[] stamped = renderer.render(html, "journal", 2025, "仕訳帳", new TaxReturnPdfRenderer.OutputStamp(
                java.time.Instant.parse("2026-03-01T01:00:00Z"), "R07-NTA0KOA21-R5-CHK20261112", "v2", "abcdef012345"));
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(stamped))) {
            byte[] content = reader.getPageContent(1);
            require(contains(content, utf16("2026-03-01 10:00:00 JST")), "記録と同じ生成日時がPDFにありません");
            require(contains(content, utf16("R07-NTA0KOA21-R5-CHK20261112")), "様式参照版がPDFにありません");
            require(contains(content, utf16("abcdef012345")), "データ指紋がPDFにありません");
        }

        boolean rejectedMissingGroup = false;
        try {
            renderer.render(html, "unknown", 2026, "unknown");
        } catch (PdfContentUnavailableException expected) {
            rejectedMissingGroup = true;
        }
        require(rejectedMissingGroup, "許可された帳票セクションがないPDFを生成しました");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static byte[] utf16(String value) {
        return value.getBytes(StandardCharsets.UTF_16BE);
    }

    private static boolean contains(byte[] content, byte[] expected) {
        for (int i = 0; i <= content.length - expected.length; i++) {
            boolean matches = true;
            for (int j = 0; j < expected.length; j++) {
                if (content[i + j] != expected[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }
}
