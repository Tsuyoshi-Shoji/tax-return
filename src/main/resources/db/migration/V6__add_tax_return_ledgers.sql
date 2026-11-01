ALTER TABLE app_user
    ADD COLUMN name_kana VARCHAR(100) NULL,
    ADD COLUMN birth_date DATE NULL;

ALTER TABLE business_profile
    ADD COLUMN tax_office VARCHAR(100) NULL,
    ADD COLUMN household_head_name VARCHAR(100) NULL,
    ADD COLUMN household_head_relation VARCHAR(30) NULL,
    ADD COLUMN consumption_tax_status VARCHAR(20) NOT NULL DEFAULT 'EXEMPT',
    ADD COLUMN simplified_business_type INT NULL;

CREATE TABLE counterparty (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_profile_id BIGINT NOT NULL,
    counterparty_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_counterparty_name UNIQUE (business_profile_id, counterparty_name),
    CONSTRAINT fk_counterparty_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE income_entry
    ADD COLUMN counterparty_id BIGINT NULL,
    ADD COLUMN withholding_tax DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_category VARCHAR(20) NULL,
    ADD CONSTRAINT fk_income_entry_counterparty FOREIGN KEY (counterparty_id) REFERENCES counterparty (id);

ALTER TABLE expense_entry
    ADD COLUMN counterparty_id BIGINT NULL,
    ADD CONSTRAINT fk_expense_entry_counterparty FOREIGN KEY (counterparty_id) REFERENCES counterparty (id);

CREATE TABLE fixed_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_profile_id BIGINT NOT NULL,
    asset_name VARCHAR(100) NOT NULL,
    account_code VARCHAR(50) NOT NULL,
    acquisition_date DATE NOT NULL,
    service_start_date DATE NOT NULL,
    acquisition_cost DECIMAL(15,0) NOT NULL,
    useful_life INT NOT NULL,
    depreciation_method VARCHAR(30) NOT NULL,
    business_ratio DECIMAL(5,2) NOT NULL,
    acquisition_funding VARCHAR(30) NOT NULL,
    notes VARCHAR(255),
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fixed_asset_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tax_return_input (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    salary_revenue DECIMAL(15,0) NOT NULL DEFAULT 0,
    salary_withholding DECIMAL(15,0) NOT NULL DEFAULT 0,
    salary_payer_name VARCHAR(100),
    salary_payer_address VARCHAR(255),
    misc_revenue DECIMAL(15,0) NOT NULL DEFAULT 0,
    misc_expense DECIMAL(15,0) NOT NULL DEFAULT 0,
    misc_withholding DECIMAL(15,0) NOT NULL DEFAULT 0,
    misc_payer_name VARCHAR(100),
    prepaid_tax DECIMAL(15,0) NOT NULL DEFAULT 0,
    self_disability VARCHAR(20) NOT NULL DEFAULT 'NONE',
    widow_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    working_student BOOLEAN NOT NULL DEFAULT FALSE,
    has_spouse BOOLEAN NOT NULL DEFAULT FALSE,
    spouse_name VARCHAR(100),
    spouse_birth_date DATE,
    spouse_income DECIMAL(15,0) NOT NULL DEFAULT 0,
    spouse_disability VARCHAR(20) NOT NULL DEFAULT 'NONE',
    spouse_cohabiting BOOLEAN NOT NULL DEFAULT TRUE,
    base_period_taxable_sales DECIMAL(15,0),
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tax_return_input_fiscal_year UNIQUE (fiscal_year_id),
    CONSTRAINT fk_tax_return_input_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tax_dependent (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    dependent_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(30) NOT NULL,
    birth_date DATE NOT NULL,
    income DECIMAL(15,0) NOT NULL DEFAULT 0,
    cohabiting BOOLEAN NOT NULL DEFAULT TRUE,
    disability VARCHAR(20) NOT NULL DEFAULT 'NONE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tax_dependent_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE tax_deduction_record
    ADD COLUMN reimbursed_amount DECIMAL(15,2) NULL;

ALTER TABLE settlement_adjustment
    ADD COLUMN balances_confirmed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE settlement_adjustment SET balances_confirmed = TRUE;
