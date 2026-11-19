package org.example.features.taxreturn.service;

import java.util.Map;

import org.example.features.taxreturn.dto.JournalReviewPage;

public interface JournalReviewService {
    JournalReviewPage load(Long userId, Integer year, String status, int pageNumber);

    void confirm(Long userId, int year, Long entryId, String fingerprint);

    /**
     * 画面で選択された複数の仕訳を、表示時の内容（fingerprint）と一致する場合に限り一括で確認済みにする。
     * 1件でも確認できない場合は全件を確認しない。
     *
     * @return 新たに確認済みにした件数
     */
    int confirmAll(Long userId, int year, Map<Long, String> fingerprints);
}
