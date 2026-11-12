package org.example.features.taxreturn.retention;

import java.util.Locale;
import java.util.Set;

/**
 * 保存管理台帳の検索条件。不正値は無視して既定（全件）にする。
 */
public final class RetentionSearch {
    private static final Set<String> KINDS = Set.of("TRANSACTION", "BOOK", "EVIDENCE");
    private static final Set<String> STATUSES = Set.of("ALL", "ACTIVE", "EXPIRED", "DUE_SOON", "HELD", "MANUAL_RULE",
            "WITH_FILE", "WITHOUT_FILE", "FILE_ALERT");

    private final String keyword;
    private final String recordKind;
    private final String status;

    public RetentionSearch(String keyword, String recordKind, String status) {
        String trimmed = keyword == null ? "" : keyword.strip();
        this.keyword = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
        this.recordKind = recordKind != null && KINDS.contains(recordKind) ? recordKind : "";
        this.status = status != null && STATUSES.contains(status) ? status : "ALL";
    }

    public static RetentionSearch all() {
        return new RetentionSearch(null, null, null);
    }

    public boolean matchesText(String... values) {
        if (keyword.isEmpty()) {
            return true;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFiltered() {
        return !keyword.isEmpty() || !recordKind.isEmpty() || !"ALL".equals(status);
    }

    public String getKeyword() {
        return keyword;
    }

    public String getRecordKind() {
        return recordKind;
    }

    public String getStatus() {
        return status;
    }
}
