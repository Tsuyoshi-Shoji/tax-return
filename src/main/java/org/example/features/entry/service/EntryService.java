package org.example.features.entry.service;

import java.util.List;

import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.csv.ExpenseCsvImportPreparation;
import org.example.features.entry.csv.ExpenseCsvRow;
import org.example.features.entry.form.EntryForm;

public interface EntryService {
    EntryPageData loadPage(EntryKind kind, Long appUserId, Long editId);

    EntryPrepareResult prepareSubmission(EntryKind kind, Long appUserId, EntryForm entryForm);

    /** CSV の全行を検証する。1行でもエラーがあれば登録用コマンドは返さない（全件一括で登録するため）。 */
    ExpenseCsvImportPreparation prepareExpenseCsvImport(Long appUserId, List<ExpenseCsvRow> rows);

    EntrySaveResult save(ValidatedEntryCommand command);

    /**
     * 仕訳が未生成の未確認取引について、元の収益・支出データから仕訳（未確認）を生成する。
     * 元データがない・取引と一致しない場合は {@link org.example.features.entry.EntryConflictException} を送出する。
     */
    void generateMissingJournal(Long appUserId, int year, Long transactionId);
}
