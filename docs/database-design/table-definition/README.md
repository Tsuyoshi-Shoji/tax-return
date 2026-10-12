# テーブル定義書一覧

`er-diagram.md` を基に、1テーブル1ファイルで定義しています。

- `app_user.md`
- `user_setting.md`
- `business_profile.md`
- `fiscal_year.md`
- `income_category.md`
- `income_item.md`
- `expense_category.md`
- `expense_item.md`
- `receive_method.md`
- `payment_method.md`
- `income_entry.md`
- `expense_entry.md`
- `ledger_account.md`
- `counterparty.md`
- `classification_rule.md`
- `accounting_transaction.md`
- `accounting_transaction_line.md`
- `transaction_attachment.md`
- `tax_return_case.md`
- `tax_return_item.md`
- `generated_document.md`
- `ai_conversation.md`
- `ai_message.md`
- `ai_feedback.md`

## DDL自動生成

`table-definition` 配下のMarkdown定義書から、MySQL 8.0向けDDL（`CREATE TABLE`）を生成できます。

### 生成スクリプト
- `generate-ddl.ps1`

### 出力先
- `schema.mysql.sql`

### 実行例

```powershell
powershell -ExecutionPolicy Bypass -File .\docs\database-design\table-definition\generate-ddl.ps1
```

必要に応じて、入力・出力パスを指定できます。

```powershell
powershell -ExecutionPolicy Bypass -File .\docs\database-design\table-definition\generate-ddl.ps1 `
  -InputDir .\docs\database-design\table-definition `
  -OutputFile .\docs\database-design\table-definition\schema.mysql.sql
```

