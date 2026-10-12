# income_entry テーブル定義

## テーブル名
- 物理名: `income_entry`
- 論理名: 収益登録

## テーブルの目的
収益登録画面の入力データを保持する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 収益登録ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | - | 非許容 | FK -> fiscal_year.id |
| 3 | income_item_id | 収益項目ID | BIGINT | - | 非許容 | FK -> income_item.id |
| 4 | receive_method_id | 受取方法ID | BIGINT | - | 非許容 | FK -> receive_method.id |
| 5 | transaction_date | 取引日 | DATE | - | 非許容 | |
| 6 | amount | 金額 | NUMERIC(15,2) | 0.00 | 非許容 | 0以上 |
| 7 | exclude_from_return | 申告対象外フラグ | BOOLEAN | FALSE | 非許容 | |
| 8 | details | 詳細 | TEXT | - | 許容 | 自由入力 |
| 9 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 10 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

