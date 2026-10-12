# app_user テーブル定義

## テーブル名
- 物理名: `app_user`
- 論理名: ユーザー

## テーブルの目的
認証およびユーザー基本情報を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | ユーザーID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | login_id | ログインID | VARCHAR(100) | - | 非許容 | UNIQUE |
| 3 | email | メールアドレス | VARCHAR(255) | - | 非許容 | UNIQUE |
| 4 | password_hash | パスワードハッシュ | VARCHAR(255) | - | 非許容 | ハッシュのみ保存 |
| 5 | display_name | 表示名 | VARCHAR(100) | - | 非許容 | 画面表示用 |
| 6 | status | ステータス | VARCHAR(20) | 'active' | 非許容 | active/inactive/deleted |
| 7 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | 監査用 |
| 8 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | 更新時に上書き |

