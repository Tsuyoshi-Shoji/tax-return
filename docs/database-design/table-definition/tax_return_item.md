# tax_return_item テーブル定義

## テーブル名
- 物理名: `tax_return_item`
- 論理名: 申告項目

## テーブルの目的
確定申告画面および帳票出力で表示する明細項目を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 申告項目ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | tax_return_case_id | 申告ケースID | BIGINT | - | 非許容 | FK -> tax_return_case.id |
| 3 | item_code | 項目コード | VARCHAR(50) | - | 非許容 | ケース内UNIQUE |
| 4 | item_name | 項目名 | VARCHAR(200) | - | 非許容 | |
| 5 | display_order | 表示順 | INTEGER | 0 | 非許容 | |
| 6 | amount | 金額 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 7 | source_formula | 算定式 | TEXT | - | 許容 | 追跡用 |
| 8 | memo | メモ | TEXT | - | 許容 | |
| 9 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 10 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |


