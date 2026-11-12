-- 確定申告画面の任意セクションの非表示設定（セクションコードのカンマ区切り。NULL は全表示）
ALTER TABLE user_setting
    ADD COLUMN hidden_tax_return_sections VARCHAR(500) NULL;
