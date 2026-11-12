# fixed_asset_event テーブル定義

## テーブルの目的

固定資産の事業専用割合変更、売却、除却、廃業、相続・贈与による承継、償却方法変更を記録し、未確定年度の減価償却・仕訳と貸借対照表の再生成に使用する。償却要件・履歴の正確性と承認根拠は利用者確認とし、法的適格性や譲渡所得等は計算しない。

## カラム定義

| No | 物理名 | 論理名 | データ型 | NULL | 備考 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | イベントID | BIGINT | 非許容 | PK |
| 2 | fixed_asset_id | 固定資産ID | BIGINT | 非許容 | FK -> fixed_asset.id |
| 3 | event_type | イベント種別 | VARCHAR(30) | 非許容 | BUSINESS_RATIO_CHANGE / SALE / DISPOSAL |
| 4 | effective_date | 適用日 | DATE | 非許容 | 割合変更は年度初日、売却・除却は実際の処分日（供用日以降・対象年度内） |
| 5 | business_ratio | 事業専用割合 | DECIMAL(5,2) | 許容 | 割合変更時のみ。1〜100 |
| 6 | proceeds | 売却価額 | DECIMAL(15,0) | 許容 | SALE時のみ |
| 7 | opening_book_value | 引継ぎ・変更時簿価 | DECIMAL(15,0) | 許容 | INHERITANCE / GIFT / METHOD_CHANGE |
| 8 | new_depreciation_method | 変更後償却方法 | VARCHAR(30) | 許容 | METHOD_CHANGE時のみ |
| 9 | history_confirmed | 履歴・根拠確認 | BOOLEAN | 非許容 | 承継・方法変更時にTRUE |
| 10 | notes | 摘要・確認根拠 | VARCHAR(255) | 許容 | 承継・方法変更時は必須 |
| 11 | version | 版 | BIGINT | 許容 | 楽観ロック |
| 12 | created_at | 作成日時 | DATETIME | 非許容 | |
| 13 | updated_at | 更新日時 | DATETIME | 非許容 | |

`(fixed_asset_id, effective_date)` は一意。所有事業者・年度をサーバーで確認し、確定年度に影響する登録・削除を拒否する。方法変更は年初に限り、承継・変更時簿価と確認根拠を保存する。変更後の適用方法は定額法・200％定率法・250％定率法間に限定する。相続・贈与は元の取得日・取得価額・方法を維持し、引継ぎ日から簿価を起点に計算する。廃業は廃業月まで償却し、残存簿価の別処理を警告する。一括償却資産の廃業・承継は対象外。変更後は影響する未確定年分の償却要件確認を取り消す。
