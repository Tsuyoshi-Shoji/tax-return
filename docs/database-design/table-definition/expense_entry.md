# expense_entry テーブル定義

## テーブル名
- 物理名: `expense_entry`
- 論理名: 支出登録

## テーブルの目的
支出登録画面の入力データを保持する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 支出登録ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | - | 非許容 | FK -> fiscal_year.id |
| 3 | expense_item_id | 支出項目ID | BIGINT | - | 非許容 | FK -> expense_item.id |
| 4 | payment_method_id | 支払方法ID | BIGINT | - | 非許容 | FK -> payment_method.id |
| 5 | transaction_date | 取引日 | DATE | - | 非許容 | |
| 6 | amount | 金額 | NUMERIC(15,2) | 0.00 | 非許容 | 0以上 |
| 7 | exclude_from_return | 申告対象外フラグ | BOOLEAN | FALSE | 非許容 | |
| 8 | home_apportionment_rate | 家事按分率 | NUMERIC(5,2) | 0.00 | 非許容 | 0.00-100.00 |
| 9 | deductible_amount | 経費対象金額 | NUMERIC(15,2) | 0.00 | 非許容 | 按分後金額 |
| 10 | details | 詳細 | TEXT | - | 許容 | 自由入力 |
| 11 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 12 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

