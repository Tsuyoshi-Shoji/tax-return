CREATE TABLE transaction_settlement_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year_id BIGINT NOT NULL,
    source_transaction_id BIGINT NOT NULL,
    settlement_transaction_id BIGINT NOT NULL,
    settled_amount DECIMAL(15,2) NOT NULL,
    fee_amount DECIMAL(15,2) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tx_settlement_event_fiscal_year
        FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year (id),
    CONSTRAINT fk_tx_settlement_event_source_tx
        FOREIGN KEY (source_transaction_id) REFERENCES accounting_transaction (id),
    CONSTRAINT fk_tx_settlement_event_settlement_tx
        FOREIGN KEY (settlement_transaction_id) REFERENCES accounting_transaction (id),
    CONSTRAINT uk_tx_settlement_event_fy_idempotency
        UNIQUE (fiscal_year_id, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

