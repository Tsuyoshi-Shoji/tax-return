# payment_method テーブル定義

## テーブル名
- 物理名: `payment_method`
- 論理名: 支払方法

## テーブルの目的
支出登録で使用する支払方法を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 支払方法ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | method_code | 方法コード | VARCHAR(50) | - | 非許容 | UNIQUE |
| 3 | method_name | 方法名 | VARCHAR(100) | - | 非許容 | |
| 4 | is_active | 有効フラグ | BOOLEAN | TRUE | 非許容 | |
| 5 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 6 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |

