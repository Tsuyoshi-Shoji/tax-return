ALTER TABLE fixed_asset ADD COLUMN asset_origin VARCHAR(30) NOT NULL DEFAULT 'ACQUISITION';
ALTER TABLE fixed_asset ADD COLUMN parent_asset_id BIGINT NULL;
ALTER TABLE fixed_asset ADD CONSTRAINT fk_fixed_asset_parent
    FOREIGN KEY (parent_asset_id) REFERENCES fixed_asset (id);

ALTER TABLE fixed_asset_event ADD COLUMN opening_book_value DECIMAL(15,0) NULL;
ALTER TABLE fixed_asset_event ADD COLUMN new_depreciation_method VARCHAR(30) NULL;
ALTER TABLE fixed_asset_event ADD COLUMN history_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE fixed_asset_event DROP CONSTRAINT chk_fixed_asset_event_type;
ALTER TABLE fixed_asset_event ADD CONSTRAINT chk_fixed_asset_event_type CHECK (
    event_type IN ('BUSINESS_RATIO_CHANGE', 'SALE', 'DISPOSAL', 'INHERITANCE', 'GIFT', 'METHOD_CHANGE', 'BUSINESS_CESSATION')
);
ALTER TABLE fixed_asset_event DROP CONSTRAINT chk_fixed_asset_event_data;
ALTER TABLE fixed_asset_event ADD CONSTRAINT chk_fixed_asset_event_data CHECK (
    (event_type = 'BUSINESS_RATIO_CHANGE' AND business_ratio > 0 AND business_ratio <= 100
        AND proceeds IS NULL AND opening_book_value IS NULL AND new_depreciation_method IS NULL)
    OR (event_type = 'SALE' AND business_ratio IS NULL AND proceeds >= 0
        AND opening_book_value IS NULL AND new_depreciation_method IS NULL)
    OR (event_type = 'DISPOSAL' AND business_ratio IS NULL AND proceeds IS NULL
        AND opening_book_value IS NULL AND new_depreciation_method IS NULL)
    OR (event_type IN ('INHERITANCE', 'GIFT') AND business_ratio IS NULL AND proceeds IS NULL
        AND opening_book_value >= 0 AND new_depreciation_method IS NULL AND history_confirmed = TRUE)
    OR (event_type = 'METHOD_CHANGE' AND business_ratio IS NULL AND proceeds IS NULL
        AND opening_book_value >= 0 AND new_depreciation_method IN ('STRAIGHT_LINE', 'DECLINING_200', 'DECLINING_250')
        AND history_confirmed = TRUE)
    OR (event_type = 'BUSINESS_CESSATION' AND business_ratio IS NULL AND proceeds IS NULL
        AND opening_book_value IS NULL AND new_depreciation_method IS NULL)
);
