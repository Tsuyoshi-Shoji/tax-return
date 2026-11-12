package org.example.features.taxreturn.retention;

import java.time.LocalDate;

public class RetentionJournalOption {
    private final Long id;
    private final LocalDate entryDate;
    private final String summary;

    public RetentionJournalOption(Long id, LocalDate entryDate, String summary) {
        this.id = id;
        this.entryDate = entryDate;
        this.summary = summary;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public String getSummary() {
        return summary;
    }
}
