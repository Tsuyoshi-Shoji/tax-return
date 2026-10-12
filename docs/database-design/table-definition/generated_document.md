# generated_document テーブル定義

## テーブル名
- 物理名: `generated_document`
- 論理名: 生成帳票

## テーブルの目的
申告書、決算書、レポートなどの出力ファイルを管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 帳票ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | tax_return_case_id | 申告ケースID | BIGINT | - | 非許容 | FK -> tax_return_case.id |
| 3 | document_type | 帳票種別 | VARCHAR(50) | - | 非許容 | 申告書/PDF/CSV等 |
| 4 | file_name | ファイル名 | VARCHAR(255) | - | 非許容 | |
| 5 | storage_path | 保存パス | VARCHAR(1000) | - | 非許容 | |
| 6 | mime_type | MIMEタイプ | VARCHAR(100) | - | 非許容 | |
| 7 | file_hash | ファイルハッシュ | VARCHAR(128) | - | 非許容 | |
| 8 | generated_at | 生成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 9 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 10 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |


