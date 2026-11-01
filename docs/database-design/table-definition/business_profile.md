# business_profile テーブル定義

## テーブル名
- 物理名: `business_profile`
- 論理名: 事業者プロフィール

## テーブルの目的
申告・帳簿作成に必要な事業者情報を管理する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 事業者ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | app_user_id | ユーザーID | BIGINT | - | 非許容 | FK -> app_user.id, UNIQUE |
| 3 | business_name | 屋号 | VARCHAR(200) | - | 許容 | |
| 4 | address | 住所 | VARCHAR(500) | - | 許容 | |
| 5 | tax_office_name | 税務署名 | VARCHAR(200) | - | 許容 | |
| 6 | filing_type | 申告方式 | VARCHAR(50) | - | 非許容 | 青色/白色など |
| 7 | bookkeeping_method | 帳簿方式 | VARCHAR(50) | - | 許容 | 65万円控除など |
| 8 | tax_notification_number | 届出番号 | VARCHAR(100) | - | 許容 | |
| 9 | start_date | 事業開始日 | DATE | - | 許容 | |
| 10 | end_date | 事業終了日 | DATE | - | 許容 | 廃業時のみ |
| 11 | created_at | 作成日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 12 | updated_at | 更新日時 | TIMESTAMP | CURRENT_TIMESTAMP | 非許容 | |
| 13 | tax_office | 提出先税務署名 | VARCHAR(100) | - | 許容 | 例: 渋谷税務署 |
| 14 | household_head_name | 世帯主の氏名 | VARCHAR(100) | - | 許容 | 申告書第一表用、個人情報 |
| 15 | household_head_relation | 世帯主との続柄 | VARCHAR(30) | - | 許容 | 申告書第一表用 |
| 16 | consumption_tax_status | 消費税の区分 | VARCHAR(20) | 'EXEMPT' | 非許容 | EXEMPT（免税事業者）/ SIMPLIFIED（簡易課税） |
| 17 | simplified_business_type | 簡易課税の事業区分 | INT | - | 許容 | 1〜6（第1種90%〜第6種40%）。SIMPLIFIED時のみ設定 |

