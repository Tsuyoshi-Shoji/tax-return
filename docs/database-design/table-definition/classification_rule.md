# classification_rule テーブル定義

## テーブル名
- 物理名: `classification_rule`
- 論理名: 自動仕分け規則

## テーブルの目的
入力データから勘定科目を提案する規則を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 規則ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | business_profile_id | 事業者ID | BIGINT | - | 非許容 | FK -> business_profile.id |
| 3 | rule_name | 規則名 | VARCHAR(100) | - | 非許容 | |
| 4 | match_type | 条件種別 | VARCHAR(30) | - | 非許容 | keyword/counterparty等 |
| 5 | match_value | 条件値 | VARCHAR(255) | - | 非許容 | |
| 6 | ledger_account_id | 勘定科目ID | BIGINT | - | 非許容 | FK -> ledger_account.id |
| 7 | priority | 優先度 | INTEGER | 100 | 非許容 | 小さいほど優先 |
| 8 | enabled | 有効フラグ | BOOLEAN | TRUE | 非許容 | |
| 9 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 10 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

