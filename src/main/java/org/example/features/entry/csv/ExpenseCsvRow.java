package org.example.features.entry.csv;

/** CSVの1データ行（未検証の文字列値）。lineNumber はファイル上の物理行番号。 */
public record ExpenseCsvRow(
        int lineNumber,
        String expenseType,
        String item,
        String date,
        String amount,
        String paymentMethod,
        String excludeFromReturn,
        String homeApportionment,
        String details) {
}
