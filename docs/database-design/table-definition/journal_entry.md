# journal_entry テーブル定義

## テーブルの目的

年度内の仕訳ヘッダーを保持し、仕訳明細および保存管理台帳の取引・証憑記録から参照する。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | 仕訳ID | BIGINT | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | 非許容 | FK -> fiscal_year.id |
| 3 | accounting_transaction_id | 取引ID | BIGINT | 許容 | FK -> accounting_transaction.id |
| 4 | source_type | 入力元種別 | VARCHAR(40) | 非許容 | |
| 5 | source_id | 入力元ID | BIGINT | 許容 | |
| 6 | entry_date | 仕訳日 | DATE | 非許容 | |
| 7 | summary | 摘要 | VARCHAR(255) | 許容 | |
| 8 | status | 状態 | VARCHAR(20) | 非許容 | |
| 9 | created_at | 作成日時 | DATETIME | 非許容 | |
| 10 | updated_at | 更新日時 | DATETIME | 非許容 | |

保存管理台帳からの参照では、仕訳が台帳レコードと同一の年度に属することをサービス層で検証する。
