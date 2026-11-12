# tax_return_input テーブル定義

## テーブル名
- 物理名: `tax_return_input`
- 論理名: 年分別の申告情報

## テーブルの目的
確定申告書の作成に必要な事業所得以外の所得・本人と配偶者の状況・予定納税額・消費税の基準期間の課税売上高を年分ごとに保持する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | - | 非許容 | FK -> fiscal_year.id、UNIQUE |
| 3 | salary_revenue | 給与の収入金額 | DECIMAL(15,0) | 0 | 非許容 | |
| 4 | salary_withholding | 給与の源泉徴収税額 | DECIMAL(15,0) | 0 | 非許容 | |
| 5 | salary_payer_name | 給与の支払者 | VARCHAR(100) | - | 許容 | |
| 6 | salary_payer_address | 給与の支払者の所在地 | VARCHAR(255) | - | 許容 | |
| 7 | misc_revenue | 雑所得の収入金額 | DECIMAL(15,0) | 0 | 非許容 | |
| 8 | misc_expense | 雑所得の必要経費 | DECIMAL(15,0) | 0 | 非許容 | |
| 9 | misc_withholding | 雑所得の源泉徴収税額 | DECIMAL(15,0) | 0 | 非許容 | |
| 10 | misc_payer_name | 雑所得の支払者 | VARCHAR(100) | - | 許容 | |
| 11 | prepaid_tax | 予定納税額 | DECIMAL(15,0) | 0 | 非許容 | |
| 12 | self_disability | 本人の障害者区分 | VARCHAR(20) | 'NONE' | 非許容 | NONE / GENERAL / SPECIAL |
| 13 | widow_status | 寡婦・ひとり親 | VARCHAR(20) | 'NONE' | 非許容 | NONE / WIDOW / SINGLE_PARENT |
| 14 | working_student | 勤労学生 | BOOLEAN | FALSE | 非許容 | |
| 15 | has_spouse | 配偶者の有無 | BOOLEAN | FALSE | 非許容 | |
| 16 | spouse_name | 配偶者の氏名 | VARCHAR(100) | - | 許容 | 個人情報（ログ出力禁止） |
| 17 | spouse_birth_date | 配偶者の生年月日 | DATE | - | 許容 | 個人情報 |
| 18 | spouse_income | 配偶者の合計所得金額 | DECIMAL(15,0) | 0 | 非許容 | |
| 19 | spouse_disability | 配偶者の障害者区分 | VARCHAR(20) | 'NONE' | 非許容 | |
| 20 | spouse_cohabiting | 配偶者と同居 | BOOLEAN | TRUE | 非許容 | |
| 21 | base_period_taxable_sales | 基準期間の課税売上高 | DECIMAL(15,0) | - | 許容 | 未入力時は2年前の帳簿から計算 |
| 22 | version | 版 | BIGINT | - | 許容 | 楽観ロック |
| 23 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 24 | updated_at | 更新日時 | DATETIME | - | 非許容 | |
| 25 | depreciation_rule_version | 償却確認ルール版 | VARCHAR(40) | - | 許容 | 年分-2026-04-01-v2。未確認・資産変更後はNULL。従来3方式・追加条件なしの場合だけv1も有効 |
| 26 | depreciation_method_basis | 償却方法の適用根拠 | VARCHAR(20) | UNCONFIRMED | 非許容 | STATUTORY / NOTIFIED / APPROVED / MIXED。定率資産にSTATUTORYのみは不可 |
| 27 | depreciation_special_confirmed | 少額特例の利用者確認 | BOOLEAN | FALSE | 非許容 | 対象者要件等の自己申告であり証明ではない |
| 28 | depreciation_business_months | 事業月数 | INT | 12 | 非許容 | 1〜12、少額特例の上限按分 |
| 29 | depreciation_evidence | 償却確認根拠 | VARCHAR(500) | - | 許容 | 種類別届出・承認等の参照メモ。秘密情報・個人情報を記入しない |
| 30 | depreciation_confirmed_at | 償却要件確認日時 | DATETIME | - | 許容 | サーバー時刻、未確認時NULL |

V11で追加。既存行へ確認済みを付与しない。所有年分・未確定状態を確認して更新し、資産・イベント変更が影響する未確定年分は版・日時をNULLにして再確認を要求する。法定適格性は自動判定しない。