# transaction_attachment テーブル定義

## テーブル名
- 物理名: `transaction_attachment`
- 論理名: 取引証憑

## テーブルの目的
取引に紐づく領収書画像やPDFなどの証憑を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 証憑ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | accounting_transaction_id | 取引ID | BIGINT | - | 非許容 | FK -> accounting_transaction.id |
| 3 | file_name | ファイル名 | VARCHAR(255) | - | 非許容 | 表示用 |
| 4 | storage_path | 保存パス | VARCHAR(1000) | - | 非許容 | 非公開ストレージ想定 |
| 5 | mime_type | MIMEタイプ | VARCHAR(100) | - | 非許容 | |
| 6 | file_hash | ファイルハッシュ | VARCHAR(128) | - | 非許容 | 改ざん検知補助 |
| 7 | captured_at | 取込日時 | TIMESTAMP | - | 許容 | |
| 8 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 9 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |


