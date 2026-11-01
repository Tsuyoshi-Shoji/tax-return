# tax_dependent テーブル定義

## テーブル名
- 物理名: `tax_dependent`
- 論理名: 扶養親族（年分別）

## テーブルの目的
扶養控除・特定親族特別控除・障害者控除・定額減税（令和6年分）の判定に使う扶養親族を年分ごとに保持する。年齢・所得は12月31日現在で判定する。個人情報のためログに出力しない。マイナンバーは保持しない。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | - | 非許容 | FK -> fiscal_year.id |
| 3 | dependent_name | 氏名 | VARCHAR(100) | - | 非許容 | |
| 4 | relationship | 続柄 | VARCHAR(30) | - | 非許容 | 「父」「母」を含み同居の70歳以上は同居老親等 |
| 5 | birth_date | 生年月日 | DATE | - | 非許容 | |
| 6 | income | 合計所得金額 | DECIMAL(15,0) | 0 | 非許容 | |
| 7 | cohabiting | 同居 | BOOLEAN | TRUE | 非許容 | |
| 8 | disability | 障害者区分 | VARCHAR(20) | 'NONE' | 非許容 | NONE / GENERAL / SPECIAL |
| 9 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 10 | updated_at | 更新日時 | DATETIME | - | 非許容 | |