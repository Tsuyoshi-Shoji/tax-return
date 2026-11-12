# report_output_record テーブル定義

## テーブルの目的

PDF（個別・ZIP一括）・CSVの出力ごとに、生成時点・計算ルール版・様式参照版・年度状態・ファイルハッシュを記録する。出力ファイル本体は保存しない。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | 出力記録ID | BIGINT | 非許容 | PK |
| 2 | fiscal_year_id | 年度ID | BIGINT | 非許容 | FK -> fiscal_year.id |
| 3 | app_user_id | 出力利用者ID | BIGINT | 非許容 | FK -> app_user.id |
| 4 | output_type | 帳票種別 | VARCHAR(30) | 非許容 | 許可リスト（pdf:各帳票 / pdf:all / csv:etax / csv:books / csv:fixed-assets） |
| 5 | file_name | ファイル名 | VARCHAR(120) | 非許容 | サーバー生成名 |
| 6 | file_sha256 | ファイルSHA-256 | VARCHAR(64) | 非許容 | |
| 7 | file_size | サイズ | BIGINT | 非許容 | バイト |
| 8 | data_fingerprint | 会計データ指紋 | VARCHAR(64) | 非許容 | 出力時点の指紋 |
| 9 | calc_rule_version | 計算ルール版 | VARCHAR(20) | 非許容 | |
| 10 | form_edition | 様式参照版 | VARCHAR(40) | 非許容 | |
| 11 | year_status | 年度状態 | VARCHAR(10) | 非許容 | CHECK: open / closed |
| 12 | close_version | 締め版番号 | INT | 許容 | 締め済み時のみ |
| 13 | record_status | 記録状態 | VARCHAR(20) | 非許容 | CHECK: CURRENT / SUPERSEDED / CANCELLED |
| 14 | cancel_reason | 取消理由 | VARCHAR(200) | 許容 | CANCELLED 時必須。ログへは出力しない |
| 15 | cancelled_at | 取消日時 | DATETIME | 許容 | |
| 16 | generated_at | 生成日時 | DATETIME | 非許容 | PDFフッターの印字と同一 |
| 17 | created_at | 作成日時 | DATETIME | 非許容 | |
| 18 | updated_at | 更新日時 | DATETIME | 非許容 | |

INDEX: (fiscal_year_id, generated_at)。記録は削除せず、締めの取消時に SUPERSEDED、利用者の取消で CANCELLED にする。記録はダウンロード転送前に作成するため、転送失敗時も記録が残る。