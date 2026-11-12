package org.example.features.taxreturn.closing;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.example.features.accounting.AccountMasterSeed;

import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.dto.TaxReturnPageData.BsRow;
import org.example.features.taxreturn.dto.TaxReturnPageData.JournalLine;
import org.example.features.taxreturn.dto.TaxReturnPageData.StatementRow;

/**
 * 年度の会計データ（決算書欄・貸借対照表・仕訳帳・控除額）の要約と指紋（SHA-256）。
 * 氏名・住所等の基本情報、エラー・警告、生成日時は含めない。同じデータからは常に同じ指紋になる。
 * <p>
 * 指紋（第2版）は様式の欄番号・欄名に依存しない（決算書欄は欄コードと金額のみ、金額0の欄は含めない。
 * 任意記載欄は科目名が会計データのため欄名を含める）。様式の改訂で欄が増減しても会計データが同じなら一致する。
 * {@code legacyFingerprint} は欄番号整合（2026-11-12）以前に締めた年度の指紋（第1版）を再現する互換用。
 */
public record YearSnapshot(
        BigDecimal sales,
        BigDecimal expenses,
        BigDecimal income,
        BigDecimal incomeAfterDeduction,
        int deduction,
        BigDecimal assetsClosing,
        BigDecimal liabilitiesClosing,
        int journalLineCount,
        BigDecimal debitTotal,
        BigDecimal creditTotal,
        String fingerprint,
        String legacyFingerprint) {

    private static final String FINGERPRINT_V2 = "fp-v2";

    public static YearSnapshot of(TaxReturnPageData page) {
        StringBuilder canonical = new StringBuilder(4096);
        StringBuilder legacy = new StringBuilder(4096);
        field(canonical, FINGERPRINT_V2);
        for (StringBuilder out : List.of(canonical, legacy)) {
            field(out, "year", page.getSelectedYear());
            field(out, "deduction", page.getDeduction());
            field(out, "sales", page.getSales());
            field(out, "expenses", page.getExpenses());
            field(out, "income", page.getIncome());
            field(out, "afterDeduction", page.getIncomeAfterDeduction());
        }
        for (StatementRow row : page.getStatementRows()) {
            if (nz(row.getAmount()).signum() != 0) {
                boolean free = AccountMasterSeed.LINE_TYPE_FREE.equals(row.getType());
                field(canonical, "stmt", row.getCode(), free ? row.getLabel() : null, row.getAmount());
            }
            if (!AccountMasterSeed.LINES_ADDED_FOR_OFFICIAL_NUMBERING.contains(row.getCode())) {
                field(legacy, "stmt", row.getCode(),
                        AccountMasterSeed.LEGACY_LINE_LABELS.getOrDefault(row.getCode(), row.getLabel()), row.getAmount());
            }
        }
        for (StringBuilder out : List.of(canonical, legacy)) {
            for (BsRow row : page.getAssets()) {
                field(out, "asset", row.getLabel(), row.getOpening(), row.getClosing());
            }
            for (BsRow row : page.getLiabilities()) {
                field(out, "liability", row.getLabel(), row.getOpening(), row.getClosing());
            }
        }
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (JournalLine line : page.getJournalAll()) {
            for (StringBuilder out : List.of(canonical, legacy)) {
                field(out, "jl", line.getDate(), line.getSummary(), line.getDebitAccount(), line.getDebitAmount(),
                        line.getCreditAccount(), line.getCreditAmount(), line.isContinuation());
            }
            debit = debit.add(nz(line.getDebitAmount()));
            credit = credit.add(nz(line.getCreditAmount()));
        }
        return new YearSnapshot(nz(page.getSales()), nz(page.getExpenses()), nz(page.getIncome()),
                nz(page.getIncomeAfterDeduction()), page.getDeduction(), nz(page.getAssetsClosing()),
                nz(page.getLiabilitiesClosing()), page.getJournalAll().size(), debit, credit,
                sha256(canonical.toString()), sha256(legacy.toString()));
    }

    /** 保存済みの指紋（第2版、または互換の第1版）と現データが一致するか。 */
    public boolean matches(String storedFingerprint) {
        return fingerprint.equals(storedFingerprint) || legacyFingerprint.equals(storedFingerprint);
    }

    public String shortFingerprint() {
        return fingerprint.substring(0, 12);
    }

    private static void field(StringBuilder out, String name, Object... values) {
        out.append(name);
        for (Object value : values) {
            out.append('\u001f');
            if (value instanceof BigDecimal amount) {
                out.append(amount.stripTrailingZeros().toPlainString());
            } else if (value != null) {
                out.append(value.toString().replace('\u001f', ' ').replace('\u001e', ' '));
            } else {
                out.append('\u0000');
            }
        }
        out.append('\u001e');
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
