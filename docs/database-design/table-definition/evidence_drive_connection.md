# evidence_drive_connection テーブル定義

## テーブルの目的

利用者ごとの Google Drive 連携（OAuth `drive.file` スコープ）の更新トークンと保管フォルダーを記録する。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | 連携ID | BIGINT | 非許容 | PK |
| 2 | app_user_id | ユーザーID | BIGINT | 非許容 | FK -> app_user.id。UNIQUE |
| 3 | encrypted_refresh_token | 暗号化更新トークン | VARCHAR(2048) | 非許容 | AES-256-GCM（`v1:`＋Base64）。鍵は環境変数 `EVIDENCE_TOKEN_KEY`。平文を保存・記録しない |
| 4 | folder_id | 保管フォルダーID | VARCHAR(200) | 非許容 | アプリが作成した Drive フォルダー |
| 5 | granted_scope | 付与スコープ | VARCHAR(500) | 非許容 | `drive.file` を含むことを連携時に確認 |
| 6 | connected_at | 連携日時 | DATETIME | 非許容 | |
| 7 | created_at | 作成日時 | DATETIME | 非許容 | |
| 8 | updated_at | 更新日時 | DATETIME | 非許容 | |

アクセストークンは保存せず、操作のたびに更新トークンから取得する。連携解除・Google 側での失効検知時は行を削除する。DBバックアップと暗号鍵は別に保管する。
