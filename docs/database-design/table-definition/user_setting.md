# user_setting テーブル定義

## テーブル名
- 物理名: `user_setting`
- 論理名: ユーザー設定

## テーブルの目的
ユーザー単位の設定値（家事按分率など）を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 設定ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | app_user_id | ユーザーID | BIGINT | - | 非許容 | FK -> app_user.id, UNIQUE |
| 3 | home_apportionment_rate | 家事按分率 | NUMERIC(5,2) | 0.00 | 非許容 | 0.00-100.00 |
| 4 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | 監査用 |
| 5 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | 更新時に上書き |

