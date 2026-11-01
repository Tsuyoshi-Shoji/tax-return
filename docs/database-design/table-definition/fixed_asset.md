# fixed_asset テーブル定義

## テーブル名
- 物理名: `fixed_asset`
- 論理名: 固定資産台帳

## テーブルの目的
事業用の減価償却資産を管理し、年分ごとの減価償却費・未償却残高を計算する（`DepreciationCalculator`）。計算結果は減価償却仕訳（`journal_entry.source_type = 'DEPRECIATION'`）と青色申告決算書3ページに反映する。

## カラム定義
| No | 物理名 | 論理名 | データ型 | 初期値 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | id | 資産ID | BIGINT | 自動採番 | 非許容 | PK |
| 2 | business_profile_id | 事業者ID | BIGINT | - | 非許容 | FK -> business_profile.id |
| 3 | asset_name | 資産の名称 | VARCHAR(100) | - | 非許容 | |
| 4 | account_code | 勘定科目コード | VARCHAR(50) | - | 非許容 | TOOLS_EQUIPMENT / BUILDING_IMPROVEMENTS / VEHICLES / SOFTWARE_ASSET / LUMP_SUM_ASSETS |
| 5 | acquisition_date | 取得年月日 | DATE | - | 非許容 | |
| 6 | service_start_date | 事業供用開始日 | DATE | - | 非許容 | 未入力時は取得年月日 |
| 7 | acquisition_cost | 取得価額 | DECIMAL(15,0) | - | 非許容 | |
| 8 | useful_life | 耐用年数 | INT | - | 非許容 | 定額法は2〜50 |
| 9 | depreciation_method | 償却方法 | VARCHAR(30) | - | 非許容 | STRAIGHT_LINE / SMALL_AMOUNT_SPECIAL（30万円未満）/ LUMP_SUM_3Y（10万円以上20万円未満） |
| 10 | business_ratio | 事業専用割合 | DECIMAL(5,2) | - | 非許容 | 1〜100 |
| 11 | acquisition_funding | 取得時の支払 | VARCHAR(30) | - | 非許容 | EXPENSE_ENTRY（支出登録の固定資産購入）/ OWNER_FUNDS（事業主借） |
| 12 | notes | 摘要 | VARCHAR(255) | - | 許容 | |
| 13 | version | 版 | BIGINT | - | 許容 | 楽観ロック |
| 14 | created_at | 作成日時 | DATETIME | - | 非許容 | |
| 15 | updated_at | 更新日時 | DATETIME | - | 非許容 | |

## 計算ルール
- 定額法：償却率＝1÷耐用年数（小数第3位切上げ）。初年度は使用開始月から12月までの月割り（1円未満切上げ）。未償却残高1円（備忘価額）まで償却。
- 事業専用割合を乗じた額（1円未満切捨て）を必要経費、残額を事業主貸とし、資産科目を直接減額する（直接法）。
- 売却・除却、定率法、特別償却には対応していない。