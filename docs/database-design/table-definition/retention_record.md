# retention_record テーブル定義

## テーブルの目的

年分別の取引・帳簿・証憑メタデータと、保存起算日・期限・根拠・保全状態を記録する。保存期限は書類種別のルール（`RetentionRules`、所得税法施行規則63条・102条、消費税法施行令50条1項、確認日 2026-11-12）でサーバーが計算するか、ルール対象外の年分・手入力では利用者が根拠資料に基づき入力する。計算は法令適合を保証せず、電子帳簿保存法への適合判定も行わない。証憑ファイル本体は保存せず、Google Drive に保管したファイルの情報は `evidence_file` に記録する。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | 保存記録ID | BIGINT | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | 非許容 | FK -> fiscal_year.id |
| 3 | journal_entry_id | 関連仕訳ID | BIGINT | 許容 | FK -> journal_entry.id。取引・証憑メタデータでは必須 |
| 4 | record_kind | 記録区分 | VARCHAR(20) | 非許容 | TRANSACTION / BOOK / EVIDENCE |
| 5 | document_type | 書類種別 | VARCHAR(100) | 非許容 | 利用者入力 |
| 6 | title | 名称 | VARCHAR(200) | 非許容 | 利用者入力。ファイル本体・パスではない |
| 7 | retention_start_date | 保存起算日 | DATE | 非許容 | ルール適用時はサーバー計算（帳簿＝翌年3月16日、書類＝作成受領年の翌年3月16日）。手入力時は利用者入力 |
| 8 | retention_deadline | 保存期限 | DATE | 非許容 | ルール適用時は該当期間の最も遅い日をサーバー計算。手入力時は利用者入力で、対象年分の最低保存期限（翌年3月16日から5年）以降に限る |
| 9 | legal_basis | 保存期限の根拠 | VARCHAR(500) | 非許容 | ルール適用時は条項・確認日・ルール版を自動生成。手入力時は対象年分・資料名・該当箇所等 |
| 10 | retention_rule_code | 保存期限ルール区分 | VARCHAR(30) | 許容 | BOOK_MAIN / BOOK_OTHER / SETTLEMENT_DOC / CASH_DOC / OTHER_DOC / MANUAL。NULL は V15 以前の手入力記録 |
| 11 | rule_version | ルール版 | VARCHAR(20) | 許容 | ルール適用時のみ（例：R2026-11-12） |
| 12 | document_date | 作成・受領日 | DATE | 許容 | 書類のルール適用時に必須（2000-01-01以降・登録日以前） |
| 13 | consumption_tax_applies | 消費税の仕入税額控除に使用 | BOOLEAN | 非許容 | 既定 FALSE。TRUE で消費税法施行令50条1項の7年を加味 |
| 14 | legal_hold | 保全中 | BOOLEAN | 非許容 | 既定 FALSE。TRUE の間は期限経過後も削除不可 |
| 15 | legal_hold_reason | 保全理由 | VARCHAR(200) | 許容 | 保全時に必須。ログには記録しない |
| 16 | legal_hold_at | 保全日時 | DATETIME | 許容 | 保全時に設定、解除時に NULL |
| 17 | created_at | 作成日時 | DATETIME | 非許容 | |
| 18 | updated_at | 更新日時 | DATETIME | 非許容 | |

`fiscal_year_id` は認証済みユーザーが所有する年度に限定する。関連仕訳も同年度のものだけを登録する。電子保存の適合状態は保持せず、アプリ画面でも適合を表示しない。保存期限内は削除・上書きしない。削除判定の期限は `retention_deadline` と対象年分の最低保存期限の遅い方とし、その翌日以降かつ保全中でない場合に限り、利用者の確認操作で紐づく証憑ファイルとともに削除できる。保全の設定・解除は所有者のみが行え、監査ログには操作・利用者ID・記録ID・結果のみを残す。
