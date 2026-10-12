# fiscal_year テーブル定義

## テーブル名
- 物理名: `fiscal_year`
- 論理名: 申告年度

## テーブルの目的
事業者ごとの会計年度と締め状態を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 年度ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | business_profile_id | 事業者ID | BIGINT | - | 非許容 | FK -> business_profile.id |
| 3 | tax_year | 申告年度 | INTEGER | - | 非許容 | 例: 2026 |
| 4 | period_start | 期間開始日 | DATE | - | 非許容 | |
| 5 | period_end | 期間終了日 | DATE | - | 非許容 | |
| 6 | status | 年度ステータス | VARCHAR(20) | 'open' | 非許容 | open/closed |
| 7 | closed_at | 締め日時 | TIMESTAMP | - | 許容 | |
| 8 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 9 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

