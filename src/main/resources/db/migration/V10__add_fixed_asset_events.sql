CREATE TABLE fixed_asset_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fixed_asset_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    effective_date DATE NOT NULL,
    business_ratio DECIMAL(5,2),
    proceeds DECIMAL(15,0),
    notes VARCHAR(255),
    version BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_fixed_asset_event_type CHECK (event_type IN ('BUSINESS_RATIO_CHANGE', 'SALE', 'DISPOSAL')),
    CONSTRAINT chk_fixed_asset_event_data CHECK (
        (event_type = 'BUSINESS_RATIO_CHANGE' AND business_ratio > 0 AND business_ratio <= 100 AND proceeds IS NULL)
        OR (event_type = 'SALE' AND business_ratio IS NULL AND proceeds >= 0)
        OR (event_type = 'DISPOSAL' AND business_ratio IS NULL AND proceeds IS NULL)
    ),
    CONSTRAINT uk_fixed_asset_event_asset_date UNIQUE (fixed_asset_id, effective_date),
    CONSTRAINT fk_fixed_asset_event_asset FOREIGN KEY (fixed_asset_id) REFERENCES fixed_asset (id),
    INDEX idx_fixed_asset_event_asset_date (fixed_asset_id, effective_date, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
