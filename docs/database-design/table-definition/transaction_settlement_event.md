# transaction_settlement_event テーブル定義

## テーブル名
- 物理名: `transaction_settlement_event`
- 論理名: 引き落とし消込イベント

## テーブルの目的
クレジットカード利用で計上した未払金について、引き落とし時点の消込イベントを記録する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | イベントID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | - | 非許容 | `fiscal_year.id` FK |
| 3 | source_transaction_id | 元取引ID | BIGINT | - | 非許容 | `accounting_transaction.id` FK（クレカ支出） |
| 4 | settlement_transaction_id | 引き落とし取引ID | BIGINT | - | 非許容 | `accounting_transaction.id` FK（引き落とし） |
| 5 | settled_amount | 消込金額 | DECIMAL(15,2) | - | 非許容 | 1円以上 |
| 6 | fee_amount | 手数料金額 | DECIMAL(15,2) | 0 | 非許容 | 0以上 |
| 7 | idempotency_key | 冪等性キー | VARCHAR(64) | - | 非許容 | 年度内で一意 |
| 8 | created_at | 作成日時 | DATETIME | CURRENT_TIMESTAMP | 非許容 | |
| 9 | updated_at | 更新日時 | DATETIME | CURRENT_TIMESTAMP | 非許容 | |

## 制約
- PK: `id`
- UNIQUE: `uk_tx_settlement_event_fy_idempotency` (`fiscal_year_id`, `idempotency_key`)
- FK: `fiscal_year_id`, `source_transaction_id`, `settlement_transaction_id`

