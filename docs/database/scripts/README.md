# RDS マスタデータ投入手順

## 前提条件

- AWS RDS 上に `private_finance_db` が既に作成されている
- テーブル（`m_expense_category` など）が既に作成されている

## 推奨: 統合実行スクリプト（最新版）

## テーブルをすべて削除して再作成する場合

既存テーブルを全削除して、現行定義で作り直す場合は以下を実行してください。

```sql
SOURCE 06_recreate_all_tables.sql;
```

その後、必要に応じてマスタデータやテストユーザーを投入してください。

```sql
SOURCE 03_clean_and_insert_master_data.sql;
SOURCE 05_drop_role_and_insert_test_user.sql;
```

既存環境に `t_income.is_business_tartget` を追加する場合は、以下を実行してください。

```sql
SOURCE 08_add_t_income_is_tax_excluded.sql;
```

既存環境で `is_tax_excluded` から `is_business_tartget` へ移行する場合は、以下を実行してください。

```sql
SOURCE 09_rename_t_income_tax_flag_to_business_tartget.sql;
```

### ステップ 1: 制約修正 → クリア → 新規投入を一括実行

**以下を順番に実行** してください：

```bash
# ステップ 1: CHECK 制約を修正
mysql -h <RDS_ENDPOINT> -u admin -p private_finance_db < 02_fix_expense_category_constraint.sql

# ステップ 2: マスタデータをクリア＆新規投入
mysql -h <RDS_ENDPOINT> -u admin -p private_finance_db < 03_clean_and_insert_master_data.sql
```

または MySQL クライアント内で：

```sql
SOURCE 02_fix_expense_category_constraint.sql;
SOURCE 03_clean_and_insert_master_data.sql;
```

### ステップ 2: 投入確認

```sql
USE private_finance_db;
SELECT COUNT(*) AS '支出区分数' FROM m_expense_category;          -- 3
SELECT COUNT(*) AS '支出項目数' FROM m_expense_subcategory;        -- 37
SELECT COUNT(*) AS '収益区分数' FROM m_income_category;            -- 4
SELECT COUNT(*) AS '収益項目数' FROM m_income_subcategory;         -- 39
SELECT COUNT(*) AS '支払方法数' FROM m_payment_method;             -- 5
SELECT COUNT(*) AS '受取方法数' FROM m_receive_method;             -- 5
```

期待値合計: **93件**

## テストユーザー投入（認証なし開発用）

`m_user.role` カラムを削除し、`user_id = 1` のテストユーザーを直接投入する場合は、以下を実行してください。

```sql
SOURCE 05_drop_role_and_insert_test_user.sql;
```

確認例:

```sql
SELECT user_id, username, email, status
FROM m_user
WHERE user_id = 1;

SELECT user_setting_id, user_id, default_business_use_ratio
FROM m_user_setting
WHERE user_id = 1;
```

## 経費系マスタのみ再投入する場合

`m_expense_category` と `m_expense_subcategory` だけを再投入する場合は、以下を実行してください。

```sql
SOURCE 07_insert_expense_master_data.sql;
```

確認例:

```sql
SELECT COUNT(*) AS expense_category_count FROM m_expense_category;       -- 3
SELECT COUNT(*) AS expense_subcategory_count FROM m_expense_subcategory; -- 37
```

## スクリプト説明

### `02_fix_expense_category_constraint.sql`
- RDS上の既存テーブルのCHECK制約を修正
- 古い制約: `('BUSINESS', 'PRIVATE', 'MIXED')`
- 新しい制約: `('BUSINESS', 'PUBLIC', 'PRIVATE')`
- 外部キーの一時削除・復旧も含める

### `03_clean_and_insert_master_data.sql`
- **既存マスタデータをすべて削除**（DELETE）
- AUTO_INCREMENTをリセット
- 新しいマスタデータを投入（37+39=76項目＋支払/受取方法）

### `01_initial_master_data.sql`（非推奨・参考用）
- 古いバージョン（重複エラーが発生する可能性あり）
- 参考資料として保持

### `05_drop_role_and_insert_test_user.sql`
- `m_user` から `role` カラムを削除
- `user_id = 1` のテストユーザーを投入または更新
- `m_user_setting` の初期設定レコードも同時に投入または更新

### `06_recreate_all_tables.sql`
- 既存テーブルをすべて削除
- `m_user`、各種マスタ、`t_expense`、`t_income` を現行定義で再作成
- 外部キー、UNIQUE、CHECK、インデックスも含めて再作成

### `07_insert_expense_master_data.sql`
- `m_expense_category` と `m_expense_subcategory` のみを削除して再投入
- 支出区分 3 件、支出サブカテゴリ 37 件を投入
- 経費系マスタだけを個別に初期化したい場合に使用

### `08_add_t_income_is_tax_excluded.sql`
- `t_income` に `is_business_tartget`（`NULL/TRUE`: 事業収支対象、`FALSE`: 事業収支対象外）を追加
- 損益集計の絞り込みに使うインデックスを追加

### `09_rename_t_income_tax_flag_to_business_tartget.sql`
- `t_income.is_tax_excluded` を `is_business_tartget` にリネーム
- 旧フラグ値（`TRUE`: 対象外）を新フラグ値（`FALSE`: 対象外）へ変換
- 損益集計向けインデックスを新名称へ再作成

## エラーが発生した場合

### "Schema-validation: missing column [client_name] in table [t_income]"

→ `t_income` に `client_name` カラムが不足しています。以下を実行してください：

```sql
SOURCE 04_add_t_income_client_name.sql;
```

### "Check constraint ... is violated"

→ ステップ 1 の `02_fix_expense_category_constraint.sql` が実行されていません。実行してください。

### "Duplicate entry '経費' for key ..."

→ 既存データが残っています。以下を実行してください：
```sql
SOURCE 03_clean_and_insert_master_data.sql;
```

### 「テーブルが存在しない」エラー

→ テーブル定義用 DDL を別途実行してから、ステップ 1 以降を実行してください。

## 注意事項

- 本番環境では事前に **backup** を取ってください
- `03_clean_and_insert_master_data.sql` は **既存マスタデータをすべて削除** します
- トランザクション中のレコード（`t_expense`, `t_income`）がある場合は別途対応してください
