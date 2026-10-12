# expense_item テーブル定義

## テーブル名
- 物理名: `expense_item`
- 論理名: 支出項目

## テーブルの目的
支出区分配下の入力項目を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 支出項目ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | expense_category_id | 支出区分ID | BIGINT | - | 非許容 | FK -> expense_category.id |
| 3 | item_code | 項目コード | VARCHAR(50) | - | 非許容 | 区分内UNIQUE |
| 4 | item_name | 項目名 | VARCHAR(100) | - | 非許容 | |
| 5 | is_active | 有効フラグ | BOOLEAN | TRUE | 非許容 | |
| 6 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 7 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

