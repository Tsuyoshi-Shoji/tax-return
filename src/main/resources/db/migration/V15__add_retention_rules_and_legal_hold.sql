ALTER TABLE retention_record
    ADD COLUMN retention_rule_code VARCHAR(30) NULL,
    ADD COLUMN rule_version VARCHAR(20) NULL,
    ADD COLUMN document_date DATE NULL,
    ADD COLUMN consumption_tax_applies BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN legal_hold_reason VARCHAR(200) NULL,
    ADD COLUMN legal_hold_at DATETIME NULL;
