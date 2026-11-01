# ui_item_account_mapping テーブル定義

## テーブル名
- 物理名: `ui_item_account_mapping`
- 論理名: 画面品目→勘定科目対応

## テーブルの目的
収益・支出登録画面の「区分 → 項目」を勘定科目へ対応付ける。固定文字列のハードコードではなく適用期間付きのマスタとして保持し、変更時は既存行に `effective_to` を設定して新しい行を追加する（業務ルール BR-ACCOUNTING-001 第10章）。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 対応ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | entry_kind | 入力種別 | VARCHAR(20) | - | 非許容 | `INCOME` / `EXPENSE` |
| 3 | category_code | 区分コード | VARCHAR(50) | - | 非許容 | `income_category.category_code` / `expense_category.category_code` |
| 4 | item_code | 項目コード | VARCHAR(50) | - | 非許容 | `income_item.item_code` / `expense_item.item_code` |
| 5 | ledger_account_id | 勘定科目ID | BIGINT | - | 非許容 | FK -> ledger_account.id |
| 6 | requires_review | 要確認フラグ | BOOLEAN | - | 非許容 | 資産計上・収益性質等の判定が必要な項目 |
| 7 | review_note | 確認事項 | VARCHAR(255) | - | 許容 | 確定申告画面の警告に表示 |
| 8 | effective_from | 適用開始日 | DATE | - | 非許容 | 種別・区分・項目・開始日でUNIQUE |
| 9 | effective_to | 適用終了日 | DATE | - | 許容 | NULLは現行 |
| 10 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 11 | updated_at | 更新日時 | DATETIME | - | 非許容 | |

## 運用
- 取引日時点で有効な対応を使用する。対応がない項目は確定申告画面で「勘定科目が未設定の品目」として警告する。
- 「その他収益」「その他支出」「その他の資金移動」は相手科目を個別判断するため初期対応を持たない。
