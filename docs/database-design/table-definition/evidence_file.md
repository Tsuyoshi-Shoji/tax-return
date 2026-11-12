# evidence_file テーブル定義

## テーブルの目的

保存管理台帳（`retention_record`）の1記録に紐づく証憑ファイル（PDF／JPEG／PNG）の外部保管先と完全性確認情報を記録する。ファイル本体は利用者本人の Google Drive に保存し、DBには保存しない。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | 証憑ファイルID | BIGINT | 非許容 | PK |
| 2 | retention_record_id | 保存記録ID | BIGINT | 非許容 | FK -> retention_record.id。UNIQUE（1記録1ファイル） |
| 3 | storage_provider | 保管先 | VARCHAR(20) | 非許容 | GOOGLE_DRIVE のみ |
| 4 | external_file_id | 外部ファイルID | VARCHAR(200) | 非許容 | Drive のファイルID。英数字・`_`・`-`のみ受け付ける |
| 5 | original_file_name | 元ファイル名 | VARCHAR(255) | 非許容 | 表示用に経路・制御文字を除去。Drive・ログへは送らない |
| 6 | mime_type | MIME種別 | VARCHAR(50) | 非許容 | CHECK: application/pdf / image/jpeg / image/png |
| 7 | file_size | サイズ | BIGINT | 非許容 | CHECK: 1〜10,485,760 バイト |
| 8 | sha256 | SHA-256 | VARCHAR(64) | 非許容 | アップロード時のハッシュ（16進小文字）。ダウンロード時に照合 |
| 9 | uploaded_at | アップロード日時 | DATETIME | 非許容 | |
| 10 | verification_status | 検証状態 | VARCHAR(20) | 非許容 | UPLOADED / OK / MISSING / TRASHED / MISMATCH |
| 11 | last_verified_at | 最終確認日時 | DATETIME | 許容 | |
| 12 | created_at | 作成日時 | DATETIME | 非許容 | |
| 13 | updated_at | 更新日時 | DATETIME | 非許容 | |

所有者は `retention_record` → `fiscal_year` → `business_profile` → `app_user` で判定し、リクエストの値を信頼しない。保存期限の経過前は削除しない。期限経過後の削除では Drive のファイルを完全削除してから本行と台帳記録を削除する。電子帳簿保存法への適合状態は保持しない。
