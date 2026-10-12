# income_item テーブル定義

## テーブル名
- 物理名: `income_item`
- 論理名: 収益項目

## テーブルの目的
収益区分配下の入力項目を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 収益項目ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | income_category_id | 収益区分ID | BIGINT | - | 非許容 | FK -> income_category.id |
| 3 | item_code | 項目コード | VARCHAR(50) | - | 非許容 | 区分内UNIQUE |
| 4 | item_name | 項目名 | VARCHAR(100) | - | 非許容 | |
| 5 | is_active | 有効フラグ | BOOLEAN | TRUE | 非許容 | |
| 6 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 7 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

