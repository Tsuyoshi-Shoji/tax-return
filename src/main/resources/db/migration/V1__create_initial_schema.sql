CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    backup_codes VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_login_id UNIQUE (login_id),
    CONSTRAINT uk_app_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NOT NULL,
    home_apportionment_rate DECIMAL(5,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_setting_app_user UNIQUE (app_user_id),
    CONSTRAINT fk_user_setting_app_user FOREIGN KEY (app_user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NOT NULL,
    business_name VARCHAR(200),
    filing_type VARCHAR(50) NOT NULL,
    bookkeeping_method VARCHAR(50),
    start_date DATE,
    end_date DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_business_profile_app_user UNIQUE (app_user_id),
    CONSTRAINT fk_business_profile_app_user FOREIGN KEY (app_user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fiscal_year (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_profile_id BIGINT NOT NULL,
    tax_year INT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    closed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fiscal_year_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE receive_method (
    id BIGINT NOT NULL AUTO_INCREMENT,
    method_code VARCHAR(50) NOT NULL,
    method_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_method (
    id BIGINT NOT NULL AUTO_INCREMENT,
    method_code VARCHAR(50) NOT NULL,
    method_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE income_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_profile_id BIGINT NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_income_category_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE expense_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_profile_id BIGINT NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_expense_category_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE income_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    income_category_id BIGINT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_income_item_income_category FOREIGN KEY (income_category_id) REFERENCES income_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE expense_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    expense_category_id BIGINT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_expense_item_expense_category FOREIGN KEY (expense_category_id) REFERENCES expense_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE accounting_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    summary VARCHAR(500),
    total_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_accounting_transaction_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE income_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    income_item_id BIGINT,
    receive_method_id BIGINT,
    accounting_transaction_id BIGINT,
    transaction_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    exclude_from_return BOOLEAN NOT NULL,
    details VARCHAR(255),
    is_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_income_entry_accounting_transaction UNIQUE (accounting_transaction_id),
    CONSTRAINT fk_income_entry_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    CONSTRAINT fk_income_entry_income_item FOREIGN KEY (income_item_id) REFERENCES income_item (id),
    CONSTRAINT fk_income_entry_receive_method FOREIGN KEY (receive_method_id) REFERENCES receive_method (id),
    CONSTRAINT fk_income_entry_accounting_transaction FOREIGN KEY (accounting_transaction_id) REFERENCES accounting_transaction (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE expense_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    expense_item_id BIGINT,
    payment_method_id BIGINT,
    accounting_transaction_id BIGINT,
    transaction_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    exclude_from_return BOOLEAN NOT NULL,
    deductible_amount DECIMAL(15,2) NOT NULL,
    home_apportionment_applied BOOLEAN NOT NULL,
    home_apportionment_rate DECIMAL(5,2),
    details VARCHAR(255),
    is_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expense_entry_accounting_transaction UNIQUE (accounting_transaction_id),
    CONSTRAINT fk_expense_entry_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    CONSTRAINT fk_expense_entry_expense_item FOREIGN KEY (expense_item_id) REFERENCES expense_item (id),
    CONSTRAINT fk_expense_entry_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method (id),
    CONSTRAINT fk_expense_entry_accounting_transaction FOREIGN KEY (accounting_transaction_id) REFERENCES accounting_transaction (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE settlement_adjustment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    opening_inventory DECIMAL(15,0) NOT NULL,
    closing_inventory DECIMAL(15,0) NOT NULL,
    depreciation DECIMAL(15,0) NOT NULL,
    accounts_receivable DECIMAL(15,0) NOT NULL,
    accounts_payable DECIMAL(15,0) NOT NULL,
    unpaid_expenses DECIMAL(15,0) NOT NULL,
    unreceived_income DECIMAL(15,0) NOT NULL,
    opening_balance DECIMAL(15,0) NOT NULL,
    closing_cash DECIMAL(15,0) NOT NULL,
    blue_deduction INT NOT NULL,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_settlement_adjustment_fiscal_year UNIQUE (fiscal_year_id),
    CONSTRAINT fk_settlement_adjustment_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
