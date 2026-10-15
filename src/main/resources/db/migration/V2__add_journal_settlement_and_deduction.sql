

ALTER TABLE accounting_transaction
    ADD COLUMN settlement_status VARCHAR(20) NULL AFTER status;

UPDATE accounting_transaction
SET settlement_status = 'SETTLED'
WHERE settlement_status IS NULL;

ALTER TABLE accounting_transaction
    MODIFY COLUMN settlement_status VARCHAR(20) NOT NULL;

ALTER TABLE settlement_adjustment
    ADD COLUMN settlement_status VARCHAR(20) NULL AFTER blue_deduction;

UPDATE settlement_adjustment
SET settlement_status = 'UNSETTLED'
WHERE settlement_status IS NULL;

ALTER TABLE settlement_adjustment
    MODIFY COLUMN settlement_status VARCHAR(20) NOT NULL;

CREATE TABLE journal_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    accounting_transaction_id BIGINT,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT,
    entry_date DATE NOT NULL,
    summary VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_journal_entry_accounting_transaction UNIQUE (accounting_transaction_id),
    CONSTRAINT uk_journal_entry_source UNIQUE (source_type, source_id),
    CONSTRAINT fk_journal_entry_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    CONSTRAINT fk_journal_entry_accounting_transaction FOREIGN KEY (accounting_transaction_id) REFERENCES accounting_transaction (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journal_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    journal_entry_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    debit_credit VARCHAR(1) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    memo VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_journal_line_entry_line_no UNIQUE (journal_entry_id, line_no),
    CONSTRAINT fk_journal_line_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entry (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tax_deduction_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    source_transaction_id BIGINT,
    deduction_type VARCHAR(60) NOT NULL,
    paid_date DATE,
    payer VARCHAR(100),
    insured_person_or_beneficiary VARCHAR(100),
    paid_amount DECIMAL(15,2) NOT NULL,
    eligible_amount DECIMAL(15,2),
    calculated_deduction_amount DECIMAL(15,2),
    evidence_id VARCHAR(120),
    notes VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    reviewed_at DATETIME,
    applied_return_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tax_deduction_source_transaction UNIQUE (source_transaction_id),
    CONSTRAINT fk_tax_deduction_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    CONSTRAINT fk_tax_deduction_source_transaction FOREIGN KEY (source_transaction_id) REFERENCES accounting_transaction (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

