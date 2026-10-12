# ai_message テーブル定義

## テーブル名
- 物理名: `ai_message`
- 論理名: AIメッセージ

## テーブルの目的
会話内のユーザー発話とAI応答を時系列で管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | メッセージID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | ai_conversation_id | 会話ID | BIGINT | - | 非許容 | FK -> ai_conversation.id |
| 3 | role | 役割 | VARCHAR(20) | - | 非許容 | user/assistant |
| 4 | message_text | 本文 | TEXT | - | 非許容 | |
| 5 | token_count | トークン数 | INTEGER | 0 | 非許容 | |
| 6 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

