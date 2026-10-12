# ai_feedback テーブル定義

## テーブル名
- 物理名: `ai_feedback`
- 論理名: AI回答評価

## テーブルの目的
AI回答に対するユーザー評価を記録する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 評価ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | ai_message_id | メッセージID | BIGINT | - | 非許容 | FK -> ai_message.id, UNIQUE |
| 3 | rating | 評価値 | SMALLINT | - | 非許容 | 1-5など |
| 4 | comment | コメント | TEXT | - | 許容 | |
| 5 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

