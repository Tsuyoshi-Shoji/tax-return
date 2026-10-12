package org.example.features.entry.service;

import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.form.EntryForm;

public interface EntryService {
    EntryPageData loadPage(EntryKind kind, Long appUserId, Long editId);

    EntryPrepareResult prepareSubmission(EntryKind kind, Long appUserId, EntryForm entryForm);

    EntrySaveResult save(ValidatedEntryCommand command);
}
