package org.example.features.entry.csv;

import java.util.List;

import org.example.features.entry.service.ValidatedEntryCommand;

/** CSV全行の検証結果。errors が空の場合のみ command が設定される。 */
public record ExpenseCsvImportPreparation(List<String> errors, ValidatedEntryCommand command) {
    public ExpenseCsvImportPreparation {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty() && command != null;
    }
}
