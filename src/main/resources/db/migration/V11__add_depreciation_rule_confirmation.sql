ALTER TABLE tax_return_input
    ADD COLUMN depreciation_rule_version VARCHAR(40) NULL,
    ADD COLUMN depreciation_method_basis VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',
    ADD COLUMN depreciation_special_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN depreciation_business_months INT NOT NULL DEFAULT 12,
    ADD COLUMN depreciation_evidence VARCHAR(500) NULL,
    ADD COLUMN depreciation_confirmed_at DATETIME NULL;
