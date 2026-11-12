# household_consumption テーブル定義

## テーブルの目的

家事消費等の年分別取引明細を保存し、対応する売上仕訳と青色申告決算書の月別・年次集計を再現する。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | 明細ID | BIGINT | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | 非許容 | FK -> fiscal_year.id |
| 3 | transaction_date | 取引日 | DATE | 非許容 | 対象年度内 |
| 4 | details | 内容 | VARCHAR(100) | 非許容 | |
| 5 | amount | 金額 | DECIMAL(15,0) | 非許容 | 1円以上 |
| 6 | version | 版 | BIGINT | 許容 | 楽観ロック |
| 7 | created_at | 作成日時 | DATETIME | 非許容 | |
| 8 | updated_at | 更新日時 | DATETIME | 非許容 | |

認証済みユーザーが所有する年度の明細に限定する。登録・削除時に未確定年度の「事業主貸／売上高」仕訳を再生成する。金額の税務上の評価額はアプリで推定しない。
