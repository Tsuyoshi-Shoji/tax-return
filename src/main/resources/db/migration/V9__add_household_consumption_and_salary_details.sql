CREATE TABLE household_consumption (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    transaction_date DATE NOT NULL,
    details VARCHAR(100) NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_household_consumption_amount CHECK (amount > 0),
    CONSTRAINT fk_household_consumption_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    INDEX idx_household_consumption_year_date (fiscal_year_id, transaction_date, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_salary_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    salary_kind VARCHAR(20) NOT NULL,
    person_name VARCHAR(100) NOT NULL,
    job_description VARCHAR(100) NOT NULL,
    work_period_start DATE NOT NULL,
    work_period_end DATE NOT NULL,
    payment_period VARCHAR(30) NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_employee_salary_kind CHECK (salary_kind IN ('SALARIES', 'DEDICATED_SALARY')),
    CONSTRAINT chk_employee_salary_amount CHECK (amount > 0),
    CONSTRAINT chk_employee_salary_period CHECK (work_period_end >= work_period_start),
    CONSTRAINT fk_employee_salary_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    INDEX idx_employee_salary_year_kind (fiscal_year_id, salary_kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
