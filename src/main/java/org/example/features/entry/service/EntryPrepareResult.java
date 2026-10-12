package org.example.features.entry.service;

import org.example.features.entry.EntryPageData;

public class EntryPrepareResult {
    private final EntryPageData pageData;
    private final ValidatedEntryCommand validatedEntry;

    public EntryPrepareResult(EntryPageData pageData, ValidatedEntryCommand validatedEntry) {
        this.pageData = pageData;
        this.validatedEntry = validatedEntry;
    }

    public EntryPageData getPageData() {
        return pageData;
    }

    public ValidatedEntryCommand getValidatedEntry() {
        return validatedEntry;
    }
}
