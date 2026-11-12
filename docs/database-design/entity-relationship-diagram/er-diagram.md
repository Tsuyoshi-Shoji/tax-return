# DB設計案

## 前提

- 個人利用を前提に、1ユーザー = 1事業者の構成を基本とする。
- 12画面（ログイン、ユーザー登録、ホーム、確定申告、収益登録、支出登録、損益一覧、損益詳細、損益レポート、損益詳細レポート、設定、AIアシスタント）の表示/入力項目を反映する。
- `screen-03-tax-return` は入力画面ではなく、集計結果と申告関連データの参照画面として扱う。
- 入力系（収益/支出）と会計仕訳系（取引ヘッダ/明細）を分離し、監査可能性を担保する。

## テーブル構成

| テーブル | 役割 | 主な内容 |
| --- | --- | --- |
| `app_user` | ログイン主体 | ログインID、メール、パスワードハッシュ、表示名、状態 |
| `user_setting` | ユーザー設定 | 家事按分率、通知設定などのユーザー単位設定 |
| `business_profile` | 事業者設定 | 屋号、住所、申告方式、帳簿方式、届出番号 |
| `fiscal_year` | 申告年度 | 年度、対象期間、締め状態 |
| `income_category` | 収益区分マスタ | 売上/雑収入などの区分 |
| `income_item` | 収益項目マスタ | 区分配下の項目 |
| `expense_category` | 支出区分マスタ | 経費/仕入などの区分 |
| `expense_item` | 支出項目マスタ | 区分配下の項目 |
| `receive_method` | 受取方法マスタ | 現金、振込など |
| `payment_method` | 支払方法マスタ | 現金、振込、カードなど |
| `income_entry` | 収益登録データ | 区分、項目、日付、金額、申告対象外、詳細 |
| `expense_entry` | 支出登録データ | 区分、項目、日付、金額、申告対象外、家事按分、詳細 |
| `ledger_account` | 勘定科目 | 勘定科目コード、種別（資産/負債/純資産/収益/費用）、正常残高（システム共通マスタ） |
| `statement_line` | 決算書様式欄 | 様式コード、欄番号、欄名、欄種別、適用年度 |
| `account_statement_mapping` | 科目→様式欄対応 | 勘定科目、様式欄（任意記載欄を含む）、適用年度 |
| `ui_item_account_mapping` | 画面品目→科目対応 | 収益/支出の区分・項目コード、勘定科目、要確認フラグ、適用期間 |
| `fixed_asset` | 固定資産台帳 | 取得日、取得価額、耐用年数、償却方法、事業専用割合、資本的支出の親資産 |
| `fixed_asset_event` | 固定資産イベント | 割合変更、売却・除却、相続・贈与、方法変更、廃業、適用日、簿価 |
| `tax_return_input` | 年分別の申告情報 | 給与・雑所得、本人・配偶者の状況、予定納税額、基準期間の課税売上高 |
| `tax_dependent` | 扶養親族（年分別） | 氏名、続柄、生年月日、所得、同居、障害者区分 |
| `counterparty` | 取引先 | 顧客、仕入先、金融機関、メモ |
| `classification_rule` | 自動仕分け規則 | キーワード、取引先、勘定科目割当 |
| `accounting_transaction` | 取引ヘッダ | 日付、摘要、金額、入力元、状態 |
| `accounting_transaction_line` | 仕訳明細 | 借方貸方、勘定科目、金額、税区分 |
| `journal_entry` | 実仕訳ヘッダ | 年度、入力元、仕訳日、摘要、状態 |
| `transaction_attachment` | 証憑 | 画像、PDF、保存先、ハッシュ値 |
| `retention_record` | 年分別保存管理台帳 | 記録区分、関連仕訳、保存起算日・期限、根拠 |
| `evidence_file` | 台帳の証憑ファイル | Drive ファイルID、MIME、サイズ、SHA-256、検証状態 |
| `fiscal_year_closing_event` | 年度締め・取消の訂正履歴 | 種別、締め版、理由、集計値、データ指紋、計算ルール版、様式参照版 |
| `report_output_record` | 帳票の出力記録 | 帳票種別、ファイルSHA-256、データ指紋、年度状態、締め版、生成日時、記録状態 |
| `evidence_drive_connection` | 利用者の Google Drive 連携 | 暗号化更新トークン、保管フォルダー、付与スコープ |
| `household_consumption` | 家事消費等の年分別明細 | 対象年度、取引日、内容、金額 |
| `employee_salary_detail` | 給与の個人別年分内訳 | 区分、氏名、職務、従事期間、支給期、支給額 |
| `tax_return_case` | 申告ドラフト | 年度、青色申告区分、集計結果、前年比、確定状態 |
| `tax_return_item` | 申告項目 | 画面表示用の項目値、順序、算定根拠 |
| `generated_document` | 帳票出力 | 申告書、決算書、レポート、出力ファイル |
| `ai_conversation` | AI会話セッション | 会話開始/終了、状態、トークン集計 |
| `ai_message` | AI会話メッセージ | user/assistantの発話本文、メタ情報 |
| `ai_feedback` | AI回答評価 | 回答への評価、コメント |

### テーブルの考え方

- `income_entry` / `expense_entry` は画面入力の事実を保持する。
- `accounting_transaction` / `accounting_transaction_line` は複式簿記の仕訳根拠を保持する。
- `tax_return_case` と `tax_return_item` は確定申告画面・帳票出力の集計スナップショットを保持する。
- `ai_conversation` と `ai_message` はAIアシスタント画面の履歴・追跡性を担保する。

## ER図

```mermaid
erDiagram
    app_user ||--|| business_profile : owns
    app_user ||--|| user_setting : has
    app_user ||--o{ ai_conversation : starts

    business_profile ||--o{ fiscal_year : has
    business_profile ||--o{ counterparty : registers
    business_profile ||--o{ classification_rule : maintains
    business_profile ||--o{ income_category : defines
    business_profile ||--o{ expense_category : defines

    income_category ||--o{ income_item : has
    expense_category ||--o{ expense_item : has

    receive_method ||--o{ income_entry : used_by
    payment_method ||--o{ expense_entry : used_by

    fiscal_year ||--o{ income_entry : contains
    fiscal_year ||--o{ expense_entry : contains
    fiscal_year ||--o{ accounting_transaction : contains
    fiscal_year ||--o{ journal_entry : contains
    fiscal_year ||--o{ retention_record : retains
    fiscal_year ||--o{ household_consumption : records
    fiscal_year ||--o{ employee_salary_detail : records
    fiscal_year ||--o{ tax_return_case : summarizes

    income_item ||--o{ income_entry : classifies
    expense_item ||--o{ expense_entry : classifies

    counterparty ||--o{ accounting_transaction : linked_to
    classification_rule ||--o{ accounting_transaction : suggests

    accounting_transaction ||--o{ accounting_transaction_line : has
    ledger_account ||--o{ accounting_transaction_line : used_by
    ledger_account ||--o{ account_statement_mapping : mapped_to
    statement_line ||--o{ account_statement_mapping : aggregates
    ledger_account ||--o{ ui_item_account_mapping : assigned_by
    business_profile ||--o{ fixed_asset : owns
    fixed_asset ||--o{ fixed_asset_event : records
    fiscal_year ||--o| tax_return_input : has
    fiscal_year ||--o{ tax_dependent : lists
    counterparty ||--o{ income_entry : pays
    counterparty ||--o{ expense_entry : receives
    accounting_transaction ||--o{ transaction_attachment : evidence
    journal_entry ||--o{ retention_record : references
    retention_record ||--o| evidence_file : stores
    app_user ||--o| evidence_drive_connection : connects
    fiscal_year ||--o{ fiscal_year_closing_event : closes
    app_user ||--o{ fiscal_year_closing_event : performs
    fiscal_year ||--o{ report_output_record : outputs
    app_user ||--o{ report_output_record : downloads

    income_entry ||--o| accounting_transaction : posted_to
    expense_entry ||--o| accounting_transaction : posted_to

    tax_return_case ||--o{ tax_return_item : consists_of
    tax_return_case ||--o{ generated_document : outputs

    ai_conversation ||--o{ ai_message : has
    ai_message ||--o| ai_feedback : evaluated_by

    journal_entry {
        BIGINT id PK
        BIGINT fiscal_year_id FK
    }

    retention_record {
        BIGINT id PK
        BIGINT fiscal_year_id FK
        BIGINT journal_entry_id FK
        VARCHAR record_kind
        DATE retention_start_date
        DATE retention_deadline
        VARCHAR legal_basis
        VARCHAR retention_rule_code
        DATE document_date
        BOOLEAN legal_hold
    }

    evidence_file {
        BIGINT id PK
        BIGINT retention_record_id FK
        VARCHAR external_file_id
        VARCHAR mime_type
        BIGINT file_size
        VARCHAR sha256
        VARCHAR verification_status
    }

    evidence_drive_connection {
        BIGINT id PK
        BIGINT app_user_id FK
        VARCHAR encrypted_refresh_token
        VARCHAR folder_id
    }

    fiscal_year_closing_event {
        BIGINT id PK
        BIGINT fiscal_year_id FK
        BIGINT app_user_id FK
        VARCHAR event_type
        INT close_version
        VARCHAR data_fingerprint
        VARCHAR calc_rule_version
        VARCHAR form_edition
    }

    report_output_record {
        BIGINT id PK
        BIGINT fiscal_year_id FK
        BIGINT app_user_id FK
        VARCHAR output_type
        VARCHAR file_sha256
        VARCHAR data_fingerprint
        VARCHAR record_status
        DATETIME generated_at
    }

    household_consumption {
        BIGINT id PK
        BIGINT fiscal_year_id FK
        DATE transaction_date
        VARCHAR details
        DECIMAL amount
    }

    employee_salary_detail {
        BIGINT id PK
        BIGINT fiscal_year_id FK
        VARCHAR salary_kind
        VARCHAR person_name
        DATE work_period_start
        DATE work_period_end
        VARCHAR payment_period
        DECIMAL amount
    }

    fixed_asset_event {
        BIGINT id PK
        BIGINT fixed_asset_id FK
        VARCHAR event_type
        DATE effective_date
        DECIMAL business_ratio
        DECIMAL proceeds
        DECIMAL opening_book_value
        VARCHAR new_depreciation_method
        BOOLEAN history_confirmed
    }
```

## 主要テーブル設計

### app_user

- `id`
- `login_id`
- `email`
- `password_hash`
- `display_name`
- `status`
- `created_at`
- `updated_at`

### user_setting

- `id`
- `app_user_id`
- `home_apportionment_rate`
- `created_at`
- `updated_at`

### business_profile

- `id`
- `app_user_id`
- `business_name`
- `address`
- `tax_office_name`
- `filing_type`
- `bookkeeping_method`
- `tax_notification_number`
- `start_date`
- `end_date`
- `created_at`
- `updated_at`

### fiscal_year

- `id`
- `business_profile_id`
- `tax_year`
- `period_start`
- `period_end`
- `status`
- `closed_at`
- `created_at`
- `updated_at`

### income_category

- `id`
- `business_profile_id`
- `category_code`
- `category_name`
- `is_active`
- `created_at`
- `updated_at`

### income_item

- `id`
- `income_category_id`
- `item_code`
- `item_name`
- `is_active`
- `created_at`
- `updated_at`

### expense_category

- `id`
- `business_profile_id`
- `category_code`
- `category_name`
- `is_active`
- `created_at`
- `updated_at`

### expense_item

- `id`
- `expense_category_id`
- `item_code`
- `item_name`
- `is_active`
- `created_at`
- `updated_at`

### receive_method

- `id`
- `method_code`
- `method_name`
- `is_active`
- `created_at`
- `updated_at`

### payment_method

- `id`
- `method_code`
- `method_name`
- `is_active`
- `created_at`
- `updated_at`

### income_entry

- `id`
- `fiscal_year_id`
- `income_item_id`
- `receive_method_id`
- `transaction_date`
- `amount`
- `exclude_from_return`
- `details`
- `created_at`
- `updated_at`

### expense_entry

- `id`
- `fiscal_year_id`
- `expense_item_id`
- `payment_method_id`
- `transaction_date`
- `amount`
- `exclude_from_return`
- `home_apportionment_rate`
- `deductible_amount`
- `details`
- `created_at`
- `updated_at`

### accounting_transaction

- `id`
- `fiscal_year_id`
- `counterparty_id`
- `classification_rule_id`
- `source_type` (`income_entry` / `expense_entry`)
- `source_id`
- `transaction_date`
- `transaction_type`
- `summary`
- `total_amount`
- `reconciliation_status`
- `status`
- `created_at`
- `updated_at`

### accounting_transaction_line

- `id`
- `accounting_transaction_id`
- `line_no`
- `ledger_account_id`
- `side`
- `amount`
- `tax_category`
- `memo`
- `created_at`
- `updated_at`

### transaction_attachment

- `id`
- `accounting_transaction_id`
- `file_name`
- `storage_path`
- `mime_type`
- `file_hash`
- `captured_at`
- `created_at`
- `updated_at`

### classification_rule

- `id`
- `business_profile_id`
- `rule_name`
- `match_type`
- `match_value`
- `ledger_account_id`
- `priority`
- `enabled`
- `created_at`
- `updated_at`

### tax_return_case

- `id`
- `fiscal_year_id`
- `revision_no`
- `blue_return_type`
- `calculation_basis`
- `status`
- `gross_sales_amount`
- `necessary_expense_amount`
- `business_income_amount`
- `blue_deduction_amount`
- `taxable_income_amount`
- `prior_year_sales_amount`
- `prior_year_expense_amount`
- `calculated_at`
- `created_at`
- `updated_at`

### tax_return_item

- `id`
- `tax_return_case_id`
- `item_code`
- `item_name`
- `display_order`
- `amount`
- `source_formula`
- `memo`
- `created_at`
- `updated_at`

### generated_document

- `id`
- `tax_return_case_id`
- `document_type`
- `file_name`
- `storage_path`
- `mime_type`
- `file_hash`
- `generated_at`
- `created_at`
- `updated_at`

### ai_conversation

- `id`
- `app_user_id`
- `fiscal_year_id`
- `status`
- `started_at`
- `ended_at`
- `total_message_count`
- `total_token_count`
- `created_at`
- `updated_at`

### ai_message

- `id`
- `ai_conversation_id`
- `role` (`user` / `assistant`)
- `message_text`
- `token_count`
- `created_at`

### ai_feedback

- `id`
- `ai_message_id`
- `rating`
- `comment`
- `created_at`

## 推奨ユニーク制約・索引

- `app_user.login_id` UNIQUE
- `app_user.email` UNIQUE
- `business_profile.app_user_id` UNIQUE
- `fiscal_year.business_profile_id, tax_year` UNIQUE
- `income_category.business_profile_id, category_code` UNIQUE
- `income_item.income_category_id, item_code` UNIQUE
- `expense_category.business_profile_id, category_code` UNIQUE
- `expense_item.expense_category_id, item_code` UNIQUE
- `ledger_account.account_code` UNIQUE
- `statement_line.form_code, line_code, effective_from_year` UNIQUE
- `account_statement_mapping.ledger_account_id, form_code, effective_from_year` UNIQUE
- `ui_item_account_mapping.entry_kind, category_code, item_code, effective_from` UNIQUE
- `income_entry.fiscal_year_id, transaction_date`
- `expense_entry.fiscal_year_id, transaction_date`
- `accounting_transaction.source_type, accounting_transaction.source_id` INDEX
- `accounting_transaction_line.accounting_transaction_id, line_no` UNIQUE
- `tax_return_case.fiscal_year_id, revision_no` UNIQUE
- `tax_return_item.tax_return_case_id, item_code` UNIQUE
- `generated_document.tax_return_case_id, document_type` UNIQUE
- `ai_message.ai_conversation_id, created_at`

## 補足

- 収益/支出登録は画面要件に合わせて `income_entry` / `expense_entry` を直接保持し、必要時に仕訳テーブルへ転記する。
- レポート（画面08/09）は原則 `income_entry` / `expense_entry` から集計し、申告系は `tax_return_case` / `tax_return_item` を参照する。
- AI画面の会話履歴は機密データを含み得るため、保持期間・マスキング・アクセス制御は運用ルールに従って実装する。
