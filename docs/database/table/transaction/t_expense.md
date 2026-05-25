## テーブル定義書

本テーブル定義書は、支出明細を管理する `t_expense` テーブルの構造を定義する。

支出登録、検索、更新、損益計算、レポート、確定申告書作成時の経費集計に使用する。

## テーブル名

### t_expense

| No | 論理名        | 物理名                    | 型             | PK | NN | UQ | デフォルト                                         | 説明                                                                                       |
|----|------------|------------------------|---------------|----|----|----|-----------------------------------------------|------------------------------------------------------------------------------------------|
| 1  | 支出ID       | expense_id             | BIGINT        | ○  | ○  | ○  | AUTO_INCREMENT                                | 支出明細の一意識別子。                                                                              |
| 2  | ユーザーID     | user_id                | BIGINT        |    | ○  |    |                                               | `m_user.user_id` を参照する外部キー。                                                              |
| 3  | 支出カテゴリID   | expense_category_id    | BIGINT        |    | ○  |    |                                               | `m_expense_category.expense_category_id` を参照する外部キー。                                      |
| 4  | 支出サブカテゴリID | expense_subcategory_id | BIGINT        |    |    |    | NULL                                          | `m_expense_subcategory.expense_subcategory_id` を参照する外部キー。未分類を許容する。                       |
| 5  | 支払方法ID     | payment_method_id      | BIGINT        |    |    |    | NULL                                          | `m_payment_method.payment_method_id` を参照する外部キー。                                          |
| 6  | 支出発生日      | expense_date           | DATE          |    | ○  |    |                                               | 支出が発生した日付。                                                                               |
| 7  | 金額         | amount                 | DECIMAL(15,0) |    | ○  |    |                                               | 支出金額。Java 側では `BigDecimal` で扱う。                                                          |
| 8  | 支出詳細       | description            | VARCHAR(500)  |    |    |    | NULL                                          | 一覧・帳票向けの簡潔な説明。                                                                           |
| 9  | メモ         | memo                   | TEXT          |    |    |    | NULL                                          | 補足メモ。                                                                                    |
| 10 | 経費対象金額     | deductible_amount      | DECIMAL(15,0) |    |    |    | NULL                                          | 税務上の経費対象金額。未設定時は Service 層で `m_user_setting.default_business_use_ratio` を使い、明示的な丸めで計算する。 |
| 11 | 削除フラグ      | deleted_flag           | BOOLEAN       |    | ○  |    | FALSE                                         | 論理削除状態。TRUE のデータは通常集計から除外する。                                                             |
| 12 | 削除日時       | deleted_at             | DATETIME      |    |    |    | NULL                                          | 論理削除日時。                                                                                  |
| 13 | 登録日時       | created_at             | DATETIME      |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                                                                                |
| 14 | 更新日時       | updated_at             | DATETIME      |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                                                                                |
| 15 | バージョン      | version                | BIGINT        |    | ○  |    | 0                                             | 楽観ロック用バージョン。JPA の `@Version` に対応する。                                                      |

## 制約

| 種別          | 対象                     | 制約名                             | 内容                                                                 |
|-------------|------------------------|---------------------------------|--------------------------------------------------------------------|
| PRIMARY KEY | expense_id             | pk_t_expense                    | 支出IDを主キーとする。                                                       |
| FOREIGN KEY | user_id                | fk_t_expense_user               | `m_user.user_id` を参照する。                                            |
| FOREIGN KEY | expense_category_id    | fk_t_expense_category           | `m_expense_category.expense_category_id` を参照する。                    |
| FOREIGN KEY | expense_subcategory_id | fk_t_expense_subcategory        | `m_expense_subcategory.expense_subcategory_id` を参照する。              |
| FOREIGN KEY | payment_method_id      | fk_t_expense_payment_method     | `m_payment_method.payment_method_id` を参照する。                        |
| CHECK       | amount                 | ck_t_expense_amount_positive    | `amount >= 0` とする。                                                 |

## 推奨インデックス

| インデックス名                           | 対象カラム                        | 用途                   |
|-----------------------------------|------------------------------|----------------------|
| idx_t_expense_user_date           | user_id, expense_date        | ユーザー別・期間別の支出検索、損益集計。 |
| idx_t_expense_user_category       | user_id, expense_category_id | ユーザー別・カテゴリ別集計。       |
| idx_t_expense_user_payment_method | user_id, payment_method_id   | ユーザー別・支払方法別集計。       |
| idx_t_expense_user_deleted        | user_id, deleted_flag        | 通常表示対象の絞り込み。         |

## 備考

- `user_id` に UNIQUE 制約は付与しない。1ユーザーが複数の支出明細を登録できる必要があるため。
- 更新時は Service 層でトランザクション境界を管理し、`version` による楽観ロックで後勝ち更新を防止する。
- 削除操作は原則として物理削除ではなく `deleted_flag` を TRUE にする論理削除とする。
- 支出データ取得時は、必ずログインユーザーの `user_id` を条件に含め、他ユーザーの会計データを参照できないようにする。
- 家事按分率は支出明細ごとには保持せず、設定画面で編集する `m_user_setting.default_business_use_ratio` を固定値として使用する。
- `deductible_amount` を保存する場合は、`amount` と `m_user_setting.default_business_use_ratio` の整合性が崩れないよう Service 層で計算・検証する。
- 設定画面で家事按分率を変更しても、既存の `deductible_amount` は自動更新しない。再計算が必要な場合は、Service 層で対象期間・対象ユーザーを明確にした再計算処理として実行する。
