# counterparty テーブル定義

## テーブル名
- 物理名: `counterparty`
- 論理名: 取引先

## テーブルの目的
取引ヘッダに紐づく取引先情報を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 取引先ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | business_profile_id | 事業者ID | BIGINT | - | 非許容 | FK -> business_profile.id |
| 3 | counterparty_name | 取引先名 | VARCHAR(200) | - | 非許容 | |
| 4 | counterparty_type | 取引先種別 | VARCHAR(30) | - | 許容 | 顧客/仕入先/金融機関 |
| 5 | note | メモ | TEXT | - | 許容 | |
| 6 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 7 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

