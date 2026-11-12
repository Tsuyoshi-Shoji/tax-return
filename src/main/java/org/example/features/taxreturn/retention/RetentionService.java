package org.example.features.taxreturn.retention;

import java.util.List;

public interface RetentionService {
    RetentionPageData load(Long appUserId, Integer taxYear);

    RetentionPageData search(Long appUserId, Integer taxYear, RetentionSearch search, java.time.LocalDate today);

    default List<String> add(Long appUserId, Integer taxYear, RetentionRecordForm form) {
        return add(appUserId, taxYear, form, java.time.LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")));
    }

    List<String> add(Long appUserId, Integer taxYear, RetentionRecordForm form, java.time.LocalDate today);

    /** 税務調査・更正の請求・紛争等のため、保存期限経過後も削除できないよう保全する。 */
    List<String> placeLegalHold(Long appUserId, Long recordId, String reason);

    List<String> releaseLegalHold(Long appUserId, Long recordId);
}
