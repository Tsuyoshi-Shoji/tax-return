CREATE TABLE ledger_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_class VARCHAR(20) NOT NULL,
    normal_balance VARCHAR(1) NOT NULL,
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ledger_account_code UNIQUE (account_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE statement_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    form_code VARCHAR(50) NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    line_number INT NOT NULL,
    line_label VARCHAR(100) NOT NULL,
    line_type VARCHAR(20) NOT NULL,
    display_order INT NOT NULL,
    effective_from_year INT NOT NULL,
    effective_to_year INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_statement_line UNIQUE (form_code, line_code, effective_from_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE account_statement_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ledger_account_id BIGINT NOT NULL,
    form_code VARCHAR(50) NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    effective_from_year INT NOT NULL,
    effective_to_year INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_account_statement_mapping UNIQUE (ledger_account_id, form_code, effective_from_year),
    CONSTRAINT fk_account_statement_mapping_account FOREIGN KEY (ledger_account_id) REFERENCES ledger_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ui_item_account_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entry_kind VARCHAR(20) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    ledger_account_id BIGINT NOT NULL,
    requires_review BOOLEAN NOT NULL,
    review_note VARCHAR(255),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ui_item_account_mapping UNIQUE (entry_kind, category_code, item_code, effective_from),
    CONSTRAINT fk_ui_item_account_mapping_account FOREIGN KEY (ledger_account_id) REFERENCES ledger_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE journal_line
    ADD COLUMN account_code VARCHAR(50) NULL AFTER debit_credit;
