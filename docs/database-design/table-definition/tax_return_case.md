# tax_return_case テーブル定義

## テーブル名
- 物理名: `tax_return_case`
- 論理名: 申告ドラフト

## テーブルの目的
年度ごとの確定申告集計結果スナップショットを管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 申告ケースID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | - | 非許容 | FK -> fiscal_year.id |
| 3 | revision_no | リビジョン番号 | INTEGER | 1 | 非許容 | 年度内UNIQUE |
| 4 | blue_return_type | 青色申告区分 | VARCHAR(30) | - | 非許容 | |
| 5 | calculation_basis | 計算根拠 | TEXT | - | 許容 | 集計条件JSONなど |
| 6 | status | 状態 | VARCHAR(20) | 'draft' | 非許容 | draft/confirmed |
| 7 | gross_sales_amount | 売上金額合計 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 8 | necessary_expense_amount | 必要経費合計 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 9 | business_income_amount | 事業所得金額 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 10 | blue_deduction_amount | 青色申告特別控除額 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 11 | taxable_income_amount | 課税所得金額 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 12 | prior_year_sales_amount | 前年売上金額 | NUMERIC(15,2) | 0.00 | 非許容 | 前年比表示用 |
| 13 | prior_year_expense_amount | 前年経費金額 | NUMERIC(15,2) | 0.00 | 非許容 | 前年比表示用 |
| 14 | calculated_at | 計算日時 | TIMESTAMP | - | 許容 | |
| 15 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 16 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

