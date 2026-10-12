# ledger_account テーブル定義

## テーブル名
- 物理名: `ledger_account`
- 論理名: 勘定科目

## テーブルの目的
仕訳明細で使用する勘定科目マスタを管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 勘定科目ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | business_profile_id | 事業者ID | BIGINT | - | 非許容 | FK -> business_profile.id |
| 3 | account_code | 勘定科目コード | VARCHAR(50) | - | 非許容 | 事業者内UNIQUE |
| 4 | account_name | 勘定科目名 | VARCHAR(100) | - | 非許容 | |
| 5 | account_type | 科目種別 | VARCHAR(30) | - | 非許容 | 資産/負債/収益/費用等 |
| 6 | tax_return_line_code | 申告区分コード | VARCHAR(50) | - | 許容 | 申告項目マッピング |
| 7 | is_active | 有効フラグ | BOOLEAN | TRUE | 非許容 | |
| 8 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 9 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

