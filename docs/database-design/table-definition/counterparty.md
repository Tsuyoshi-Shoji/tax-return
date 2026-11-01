# counterparty テーブル定義

## テーブル名
- 物理名: `counterparty`
- 論理名: 取引先

## テーブルの目的
収益の支払者・支出の支払先を管理する。確定申告書第二表「所得の内訳」、青色申告決算書3ページの地代家賃・利子割引料・税理士等報酬の内訳に使用する。収益・支出登録時に取引先名で検索し、未登録なら作成する（Flyway `V6`）。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 取引先ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | business_profile_id | 事業者ID | BIGINT | - | 非許容 | FK -> business_profile.id |
| 3 | counterparty_name | 取引先名 | VARCHAR(100) | - | 非許容 | 事業者内UNIQUE |
| 4 | address | 所在地 | VARCHAR(255) | - | 許容 | 入力時のみ更新 |
| 5 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 6 | updated_at | 更新日時 | DATETIME | - | 非許容 | |

## 関連カラム
- `income_entry.counterparty_id`、`income_entry.withholding_tax`（源泉徴収税額、DECIMAL(15,2) NOT NULL DEFAULT 0）、`income_entry.tax_category`（消費税区分：TAXABLE_10 / TAXABLE_8 / NON_TAXABLE / OUT_OF_SCOPE / EXPORT_EXEMPT）
- `expense_entry.counterparty_id`