# accounting_transaction_line テーブル定義

## テーブル名
- 物理名: `accounting_transaction_line`
- 論理名: 仕訳明細

## テーブルの目的
1取引に対する借方/貸方の明細行を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 明細ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | accounting_transaction_id | 取引ID | BIGINT | - | 非許容 | FK -> accounting_transaction.id |
| 3 | line_no | 行番号 | INTEGER | - | 非許容 | 取引内UNIQUE |
| 4 | ledger_account_id | 勘定科目ID | BIGINT | - | 非許容 | FK -> ledger_account.id |
| 5 | side | 借貸区分 | VARCHAR(10) | - | 非許容 | debit/credit |
| 6 | amount | 金額 | NUMERIC(15,2) | 0.00 | 非許容 | |
| 7 | tax_category | 税区分 | VARCHAR(30) | - | 許容 | |
| 8 | memo | メモ | VARCHAR(500) | - | 許容 | |
| 9 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 10 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

