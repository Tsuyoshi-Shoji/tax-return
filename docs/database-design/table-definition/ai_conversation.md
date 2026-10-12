# ai_conversation テーブル定義

## テーブル名
- 物理名: `ai_conversation`
- 論理名: AI会話

## テーブルの目的
AIアシスタントの会話セッション単位の情報を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 会話ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | app_user_id | ユーザーID | BIGINT | - | 非許容 | FK -> app_user.id |
| 3 | fiscal_year_id | 年度ID | BIGINT | - | 許容 | FK -> fiscal_year.id |
| 4 | status | 会話状態 | VARCHAR(20) | 'active' | 非許容 | active/ended |
| 5 | started_at | 開始日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 6 | ended_at | 終了日時 | TIMESTAMP | - | 許容 | |
| 7 | total_message_count | 総メッセージ数 | INTEGER | 0 | 非許容 | |
| 8 | total_token_count | 総トークン数 | INTEGER | 0 | 非許容 | |
| 9 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 10 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

