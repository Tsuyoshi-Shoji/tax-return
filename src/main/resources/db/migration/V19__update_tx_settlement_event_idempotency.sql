ALTER TABLE transaction_settlement_event
    DROP INDEX uk_tx_settlement_event_fy_idempotency;

ALTER TABLE transaction_settlement_event
    ADD CONSTRAINT uk_tx_settlement_event_fy_idempotency_source
        UNIQUE (fiscal_year_id, idempotency_key, source_transaction_id);

