## テーブル定義書

本テーブル定義書は、収益明細を管理する `t_income` テーブルの構造を定義する。

収益登録、検索、更新、損益計算、レポート、確定申告書作成時の収益集計に使用する。

## テーブル名

### t_income

| No | 論理名        | 物理名                   | 型             | PK | NN | UQ | デフォルト                                         | 説明                                                               |
|----|------------|-----------------------|---------------|----|----|----|-----------------------------------------------|------------------------------------------------------------------|
| 1  | 収益ID       | income_id             | BIGINT        | ○  | ○  | ○  | AUTO_INCREMENT                                | 収益明細の一意識別子。                                                      |
| 2  | ユーザーID     | user_id               | BIGINT        |    | ○  |    |                                               | `m_user.user_id` を参照する外部キー。                                      |
| 3  | 収益カテゴリID   | income_category_id    | BIGINT        |    | ○  |    |                                               | `m_income_category.income_category_id` を参照する外部キー。                |
| 4  | 収益サブカテゴリID | income_subcategory_id | BIGINT        |    |    |    | NULL                                          | `m_income_subcategory.income_subcategory_id` を参照する外部キー。未分類を許容する。 |
| 5  | 受取方法ID     | receive_method_id     | BIGINT        |    |    |    | NULL                                          | `m_receive_method.receive_method_id` を参照する外部キー。                  |
| 6  | 収益発生日      | income_date           | DATE          |    | ○  |    |                                               | 収益が発生した日付。                                                       |
| 7  | 金額         | amount                | DECIMAL(15,0) |    | ○  |    |                                               | 収益金額。Java 側では `BigDecimal` で扱う。                                  |
| 8  | 取引先        | client_name           | VARCHAR(255)  |    |    |    | NULL                                          | 収益の取引先名。                                                         |
| 9  | 収益詳細       | description           | VARCHAR(500)  |    |    |    | NULL                                          | 一覧・帳票向けの簡潔な説明。                                                   |
| 10 | メモ         | memo                  | TEXT          |    |    |    | NULL                                          | 補足メモ。                                                            |
| 11 | 事業収支対象フラグ  | is_business_tartget   | BOOLEAN       |    |    |    | NULL                                          | `NULL`/`TRUE`: 事業収支対象、`FALSE`: 事業収支対象外（事業損益計算から除外）。              |
| 12 | 削除フラグ      | deleted_flag          | BOOLEAN       |    | ○  |    | FALSE                                         | 論理削除状態。TRUE のデータは通常集計から除外する。                                     |
| 13 | 削除日時       | deleted_at            | DATETIME      |    |    |    | NULL                                          | 論理削除日時。                                                          |
| 14 | 登録日時       | created_at            | DATETIME      |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                                                        |
| 15 | 更新日時       | updated_at            | DATETIME      |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                                                        |
| 16 | バージョン      | version               | BIGINT        |    | ○  |    | 0                                             | 楽観ロック用バージョン。JPA の `@Version` に対応する。                              |

## 制約

| 種別          | 対象                    | 制約名                         | 内容                                                  |
|-------------|-----------------------|-----------------------------|-----------------------------------------------------|
| PRIMARY KEY | income_id             | pk_t_income                 | 収益IDを主キーとする。                                        |
| FOREIGN KEY | user_id               | fk_t_income_user            | `m_user.user_id` を参照する。                             |
| FOREIGN KEY | income_category_id    | fk_t_income_category        | `m_income_category.income_category_id` を参照する。       |
| FOREIGN KEY | income_subcategory_id | fk_t_income_subcategory     | `m_income_subcategory.income_subcategory_id` を参照する。 |
| FOREIGN KEY | receive_method_id     | fk_t_income_receive_method  | `m_receive_method.receive_method_id` を参照する。         |
| CHECK       | amount                | ck_t_income_amount_positive | `amount >= 0` とする。                                  |

## 推奨インデックス

| インデックス名                                     | 対象カラム                                               | 用途                    |
|---------------------------------------------|-----------------------------------------------------|-----------------------|
| idx_t_income_user_date                      | user_id, income_date                                | ユーザー別・期間別の収益検索、損益集計。  |
| idx_t_income_user_category                  | user_id, income_category_id                         | ユーザー別・カテゴリ別集計。        |
| idx_t_income_user_receive_method            | user_id, receive_method_id                          | ユーザー別・受取方法別集計。        |
| idx_t_income_user_deleted                   | user_id, deleted_flag                               | 通常表示対象の絞り込み。          |
| idx_t_income_user_deleted_business_tartget_date | user_id, deleted_flag, is_business_tartget, income_date | 事業収支集計時の対象判定・期間絞り込み。 |

## 備考

- `user_id` に UNIQUE 制約は付与しない。1ユーザーが複数の収益明細を登録できる必要があるため。
- 更新時は Service 層でトランザクション境界を管理し、`version` による楽観ロックで後勝ち更新を防止する。
- 削除操作は原則として物理削除ではなく `deleted_flag` を TRUE にする論理削除とする。
- 収益データ取得時は、必ずログインユーザーの `user_id` を条件に含め、他ユーザーの会計データを参照できないようにする。
- 損益計算、レポート、申告書作成の収益集計では `is_business_tartget = FALSE` を除外し、`NULL` / `TRUE` を対象として扱う。
