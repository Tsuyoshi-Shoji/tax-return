# income_category テーブル定義

## テーブル名
- 物理名: `income_category`
- 論理名: 収益区分

## テーブルの目的
収益登録画面で選択する区分（売上/雑収入など）を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 収益区分ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | business_profile_id | 事業者ID | BIGINT | - | 非許容 | FK -> business_profile.id |
| 3 | category_code | 区分コード | VARCHAR(50) | - | 非許容 | 事業者内UNIQUE |
| 4 | category_name | 区分名 | VARCHAR(100) | - | 非許容 | |
| 5 | is_active | 有効フラグ | BOOLEAN | TRUE | 非許容 | |
| 6 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 7 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |


