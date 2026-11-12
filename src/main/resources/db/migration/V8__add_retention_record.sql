CREATE TABLE retention_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    journal_entry_id BIGINT,
    record_kind VARCHAR(20) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    retention_start_date DATE NOT NULL,
    retention_deadline DATE NOT NULL,
    legal_basis VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_retention_record_kind CHECK (record_kind IN ('TRANSACTION', 'BOOK', 'EVIDENCE')),
    CONSTRAINT chk_retention_record_dates CHECK (retention_deadline >= retention_start_date),
    CONSTRAINT chk_retention_record_journal CHECK (
        (record_kind = 'BOOK' AND journal_entry_id IS NULL)
        OR (record_kind IN ('TRANSACTION', 'EVIDENCE') AND journal_entry_id IS NOT NULL)
    ),
    CONSTRAINT fk_retention_record_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    CONSTRAINT fk_retention_record_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entry (id),
    INDEX idx_retention_record_year_deadline (fiscal_year_id, retention_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
