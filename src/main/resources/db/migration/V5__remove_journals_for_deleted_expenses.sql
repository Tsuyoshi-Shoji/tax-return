DELETE FROM journal_line
WHERE journal_entry_id IN (
    SELECT id
    FROM journal_entry
    WHERE accounting_transaction_id IN (
        SELECT accounting_transaction_id
        FROM expense_entry
        WHERE is_deleted = TRUE
          AND accounting_transaction_id IS NOT NULL
    )
);

DELETE FROM journal_entry
WHERE accounting_transaction_id IN (
    SELECT accounting_transaction_id
    FROM expense_entry
    WHERE is_deleted = TRUE
      AND accounting_transaction_id IS NOT NULL
);
