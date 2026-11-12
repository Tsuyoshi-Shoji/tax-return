package org.example.features.taxreturn.service;

import org.example.features.taxreturn.dto.JournalReviewPage;

public interface JournalReviewService {
    JournalReviewPage load(Long userId, Integer year, String status, int pageNumber);

    void confirm(Long userId, int year, Long entryId, String fingerprint);
}
