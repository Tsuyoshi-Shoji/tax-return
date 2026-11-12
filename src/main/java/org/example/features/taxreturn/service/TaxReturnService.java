package org.example.features.taxreturn.service;

import java.util.List;

import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.form.SettlementForm;

public interface TaxReturnService {
    TaxReturnPageData load(Long appUserId, Integer year, SettlementForm overrideForm);

    record BooksExport(TaxReturnPageData page, String csv) {}
    final class BookAccountOption {
        private final String code;
        private final String name;

        public BookAccountOption(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
    }

    BooksExport exportBooks(Long appUserId, Integer year);

    BooksExport exportBooks(Long appUserId, Integer year, String fromDate, String toDate, String accountCode);

    BooksExport exportFixedAssetLedger(Long appUserId, Integer year);

    BooksExport exportFixedAssetLedger(Long appUserId, Integer year, String fromDate, String toDate, String accountCode);

    List<BookAccountOption> availableBookAccounts();

    List<String> saveSettlement(Long appUserId, Integer year, SettlementForm form);

    List<String> updateDeduction(Long appUserId, Integer year, String deduction,
            boolean requirementsConfirmed, String deduction65Method);
}
