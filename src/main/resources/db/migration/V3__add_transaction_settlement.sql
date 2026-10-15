ALTER TABLE accounting_transaction
    ADD COLUMN settlement_reference_id BIGINT NULL AFTER settlement_status;

CREATE TABLE transaction_settlement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_transaction_id BIGINT NOT NULL,
    settlement_adjustment_id BIGINT NOT NULL,
    settled_amount DECIMAL(15,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transaction_settlement_source_tx
        FOREIGN KEY (source_transaction_id) REFERENCES accounting_transaction (id),
    CONSTRAINT fk_transaction_settlement_adjustment
        FOREIGN KEY (settlement_adjustment_id) REFERENCES settlement_adjustment (id),
    CONSTRAINT uk_transaction_settlement_source_adjustment
        UNIQUE (source_transaction_id, settlement_adjustment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

