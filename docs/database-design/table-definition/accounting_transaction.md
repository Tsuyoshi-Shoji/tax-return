# accounting_transaction テーブル定義

## テーブル名
- 物理名: `accounting_transaction`
- 論理名: 取引ヘッダ

## テーブルの目的
仕訳化された取引のヘッダ情報を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 取引ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | - | 非許容 | FK -> fiscal_year.id |
| 3 | counterparty_id | 取引先ID | BIGINT | - | 許容 | FK -> counterparty.id |
| 4 | classification_rule_id | 仕分け規則ID | BIGINT | - | 許容 | FK -> classification_rule.id |
| 5 | source_type | 入力元種別 | VARCHAR(30) | - | 非許容 | income_entry/expense_entry |
| 6 | source_id | 入力元ID | BIGINT | - | 非許容 | source_typeと組で参照 |
| 7 | transaction_date | 取引日 | DATE | - | 非許容 | |
| 8 | transaction_type | 取引種別 | VARCHAR(30) | - | 非許容 | income/expense/journal |
| 9 | summary | 摘要 | VARCHAR(500) | - | 許容 | |
| 10 | total_amount | 合計金額 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 11 | reconciliation_status | 照合状態 | VARCHAR(20) | 'unreconciled' | 非許容 | |
| 12 | status | ステータス | VARCHAR(20) | 'draft' | 非許容 | draft/finalized |
| 13 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 14 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |


