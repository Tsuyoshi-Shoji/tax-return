# account_statement_mapping テーブル定義

## テーブル名
- 物理名: `account_statement_mapping`
- 論理名: 勘定科目→様式欄対応

## テーブルの目的
勘定科目をどの決算書様式欄へ集約するかを年度別に管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 対応ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | ledger_account_id | 勘定科目ID | BIGINT | - | 非許容 | FK -> ledger_account.id |
| 3 | form_code | 様式コード | VARCHAR(50) | - | 非許容 | `statement_line.form_code` |
| 4 | line_code | 欄コード | VARCHAR(50) | - | 非許容 | `statement_line.line_code`、または任意記載欄へ割り当てる場合は `FREE` |
| 5 | effective_from_year | 適用開始年度 | INT | - | 非許容 | 科目・様式・開始年度でUNIQUE |
| 6 | effective_to_year | 適用終了年度 | INT | - | 許容 | |
| 7 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 8 | updated_at | 更新日時 | DATETIME | - | 非許容 | |

## 集約ルール
- `FREE` の科目は金額が0でないものを勘定科目の表示順に任意記載欄（㉕〜㉚）へ割り当て、欄名に科目名を表示する。欄が不足した科目は雑費（㉛）へ含め、警告を表示する。
- 対応がない収益科目は①売上（収入）金額、費用科目は㉛雑費へ含め、警告を表示する。
- 専従者給与は経費欄ではなく㊲（繰入額等）へ集約する。
- 期首・期末棚卸高（②⑤）は決算整理の入力値を使用する。
