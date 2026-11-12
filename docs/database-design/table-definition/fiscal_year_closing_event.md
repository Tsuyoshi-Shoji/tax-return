# fiscal_year_closing_event テーブル定義

## テーブルの目的

年度締め（CLOSE）と締めの取消（REOPEN）の訂正履歴を追記専用で記録する。各イベント時点の集計値、会計データ指紋、計算ルール版、様式参照版を保持し、再締め時の差分表示と締め後の改変検知に用いる。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | イベントID | BIGINT | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | 非許容 | FK -> fiscal_year.id |
| 3 | app_user_id | 実行利用者ID | BIGINT | 非許容 | FK -> app_user.id。認証コンテキストから設定 |
| 4 | event_type | 種別 | VARCHAR(10) | 非許容 | CHECK: CLOSE / REOPEN |
| 5 | close_version | 締め版番号 | INT | 非許容 | CHECK: 1以上。REOPEN は取消対象の締め版番号 |
| 6 | reason | 理由 | VARCHAR(200) | 許容 | 取消・再締めで必須（初回締めは任意）。ログへは出力しない |
| 7 | sales | 売上 | DECIMAL(15,2) | 非許容 | |
| 8 | expenses | 経費 | DECIMAL(15,2) | 非許容 | |
| 9 | income | 所得 | DECIMAL(15,2) | 非許容 | |
| 10 | income_after_deduction | 控除後所得 | DECIMAL(15,2) | 非許容 | |
| 11 | blue_deduction | 青色申告特別控除額 | INT | 非許容 | |
| 12 | assets_closing | 期末資産 | DECIMAL(15,2) | 非許容 | |
| 13 | liabilities_closing | 期末負債 | DECIMAL(15,2) | 非許容 | |
| 14 | journal_line_count | 仕訳行数 | INT | 非許容 | |
| 15 | debit_total | 借方合計 | DECIMAL(15,2) | 非許容 | |
| 16 | credit_total | 貸方合計 | DECIMAL(15,2) | 非許容 | |
| 17 | data_fingerprint | 会計データ指紋 | VARCHAR(64) | 非許容 | SHA-256（16進小文字）。氏名・住所は含めない |
| 18 | calc_rule_version | 計算ルール版 | VARCHAR(20) | 非許容 | |
| 19 | form_edition | 様式参照版 | VARCHAR(40) | 非許容 | 本アプリの識別子（国税庁の公式版番号ではない） |
| 20 | occurred_at | 発生日時 | DATETIME | 非許容 | |
| 21 | created_at | 作成日時 | DATETIME | 非許容 | |

UNIQUE: (fiscal_year_id, close_version, event_type)。行は更新・削除しない。所有者は `fiscal_year` → `business_profile` → `app_user` で判定する。DB管理者による改変の検知や電子帳簿保存法への適合は保証しない。