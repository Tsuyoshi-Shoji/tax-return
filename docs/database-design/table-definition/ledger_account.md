# ledger_account テーブル定義

## テーブル名
- 物理名: `ledger_account`
- 論理名: 勘定科目

## テーブルの目的
仕訳明細・損益計算書・決算書で使用する勘定科目マスタを管理する。システム共通マスタとし、初期値は業務ルール（`docs/architecture/tax-domain-rules.md` 第2章）に基づき起動時に未登録分のみ投入する。利用者別の補助科目は今後の拡張とする。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 勘定科目ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | account_code | 勘定科目コード | VARCHAR(50) | - | 非許容 | UNIQUE。例: `SALES`, `COMMUNICATION`, `OWNER_DRAW` |
| 3 | account_name | 勘定科目名 | VARCHAR(100) | - | 非許容 | |
| 4 | account_class | 大区分 | VARCHAR(20) | - | 非許容 | `ASSET` / `LIABILITY` / `EQUITY` / `REVENUE` / `EXPENSE`（事業主貸・事業主借は `EQUITY`） |
| 5 | normal_balance | 正常残高 | VARCHAR(1) | - | 非許容 | `D`（借方）/ `C`（貸方） |
| 6 | display_order | 表示順 | INT | - | 非許容 | 損益計算書・任意記載欄の割当順 |
| 7 | is_active | 有効フラグ | BOOLEAN | - | 非許容 | |
| 8 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 9 | updated_at | 更新日時 | DATETIME | - | 非許容 | |

## 関連
- 決算書の欄への対応は `account_statement_mapping`、画面品目からの対応は `ui_item_account_mapping` で管理する。
- `journal_line.account_code` に勘定科目コードを記録する（科目名は表示用に併記）。
