# statement_line テーブル定義

## テーブル名
- 物理名: `statement_line`
- 論理名: 決算書様式欄

## テーブルの目的
青色申告決算書等の様式ごとに、欄番号・欄名・欄種別を年度別に管理する。様式改正時は既存行を上書きせず、新しい `effective_from_year` の行を追加し、旧行に `effective_to_year` を設定する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 様式欄ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | form_code | 様式コード | VARCHAR(50) | - | 非許容 | 例: `BLUE_GENERAL_PL`（青色申告決算書（一般用）損益計算書） |
| 3 | line_code | 欄コード | VARCHAR(50) | - | 非許容 | 例: `SALES`, `COMMUNICATION`, `FREE_1`。様式・適用開始年度内UNIQUE |
| 4 | line_number | 欄番号 | INT | - | 非許容 | 様式上の丸数字（①=1） |
| 5 | line_label | 欄名 | VARCHAR(100) | - | 非許容 | |
| 6 | line_type | 欄種別 | VARCHAR(20) | - | 非許容 | `INPUT`（科目集計）/ `FREE`（任意記載欄）/ `CALC`（計算欄） |
| 7 | display_order | 表示順 | INT | - | 非許容 | |
| 8 | effective_from_year | 適用開始年度 | INT | - | 非許容 | |
| 9 | effective_to_year | 適用終了年度 | INT | - | 許容 | NULLは現行 |
| 10 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 11 | updated_at | 更新日時 | DATETIME | - | 非許容 | |

## 初期値
- `BLUE_GENERAL_PL`：①売上（収入）金額〜㊺所得金額の45欄（国税庁「所得税青色申告決算書（一般用）【令和5年分以降用】」（様式ID NTA0KOA21）の欄番号に一致。令和6・7年分は同一様式）。初期値は `AccountMasterSeed` で起動時に投入し、既存行は欄番号・欄名・欄種別のみ初期値へ同期する（金額・科目対応は変更しない）。2026-11-12に旧版の㉞〜㊸（繰戻額等・繰入額等の「その他」各1欄）を公式の㉞〜㊺（各2欄）へ訂正し、`REVERSAL_OTHER2`・`PROVISION_OTHER2` を追加した。
- 欄番号・欄名は対象年度の国税庁様式で確認すること（確認日・対象年度を記録する）。
