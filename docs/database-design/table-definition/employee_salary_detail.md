# employee_salary_detail テーブル定義

## テーブルの目的

給料賃金・青色事業専従者給与を個人別・年分別に集計し、決算書の印刷用内訳へ出力する。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | 明細ID | BIGINT | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | 非許容 | FK -> fiscal_year.id |
| 3 | salary_kind | 給与区分 | VARCHAR(20) | 非許容 | SALARIES / DEDICATED_SALARY |
| 4 | person_name | 氏名 | VARCHAR(100) | 非許容 | 個人情報 |
| 5 | job_description | 職務 | VARCHAR(100) | 非許容 | |
| 6 | work_period_start | 従事期間開始日 | DATE | 非許容 | 対象年度内 |
| 7 | work_period_end | 従事期間終了日 | DATE | 非許容 | 開始日以降、対象年度内 |
| 8 | payment_period | 支給期 | VARCHAR(30) | 非許容 | 利用者入力 |
| 9 | amount | 年分支給額 | DECIMAL(15,0) | 非許容 | 1円以上 |
| 10 | version | 版 | BIGINT | 許容 | 楽観ロック |
| 11 | created_at | 作成日時 | DATETIME | 非許容 | |
| 12 | updated_at | 更新日時 | DATETIME | 非許容 | |

認証済みユーザーが所有する年度の明細に限定する。区分別合計と会計上の対応科目額を照合するが、専従者該当性、届出、金額の適正性などの税務要件は判定しない。
