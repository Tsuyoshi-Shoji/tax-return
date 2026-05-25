## テーブル定義書

本テーブル定義書は、ユーザー別のアプリケーション設定を管理する `m_user_setting` テーブルの構造を定義する。

設定画面で編集可能な値をユーザー単位で保持し、支出登録・損益計算・確定申告書作成時の計算条件として使用する。

## テーブル名

### m_user_setting

| No | 論理名      | 物理名                        | 型            | PK | NN | UQ | デフォルト                                         | 説明                                             |
|----|----------|----------------------------|--------------|----|----|----|-----------------------------------------------|------------------------------------------------|
| 1  | ユーザー設定ID | user_setting_id            | BIGINT       | ○  | ○  | ○  | AUTO_INCREMENT                                | ユーザー設定の一意識別子。                                  |
| 2  | ユーザーID   | user_id                    | BIGINT       |    | ○  | ○  |                                               | `m_user.user_id` を参照する外部キー。1ユーザーにつき1設定レコードとする。 |
| 3  | 固定家事按分率  | default_business_use_ratio | DECIMAL(5,2) |    | ○  |    | 100.00                                        | 設定画面で編集する固定の事業利用割合。0.00～100.00 の範囲で管理する。       |
| 4  | 登録日時     | created_at                 | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                                      |
| 5  | 更新日時     | updated_at                 | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                                      |
| 6  | バージョン    | version                    | BIGINT       |    | ○  |    | 0                                             | 楽観ロック用バージョン。JPA の `@Version` に対応する。            |

## 制約

| 種別          | 対象                         | 制約名                                  | 内容                                                                                 |
|-------------|----------------------------|--------------------------------------|------------------------------------------------------------------------------------|
| PRIMARY KEY | user_setting_id            | pk_m_user_setting                    | ユーザー設定IDを主キーとする。                                                                   |
| FOREIGN KEY | user_id                    | fk_m_user_setting_user               | `m_user.user_id` を参照する。                                                            |
| UNIQUE      | user_id                    | uq_m_user_setting_user               | 1ユーザーにつき1設定レコードとする。                                                                |
| CHECK       | default_business_use_ratio | ck_m_user_setting_business_use_ratio | `default_business_use_ratio >= 0.00 AND default_business_use_ratio <= 100.00` とする。 |

## 推奨インデックス

| インデックス名                 | 対象カラム   | 用途             |
|-------------------------|---------|----------------|
| idx_m_user_setting_user | user_id | ログインユーザーの設定取得。 |

## 備考

- `default_business_use_ratio` は設定画面から編集可能な固定値として扱う。
- 支出登録・更新時に `t_expense.deductible_amount` を計算する場合は、Service 層でログインユーザーの `m_user_setting.default_business_use_ratio` を取得して使用する。
- 設定値の更新は Service 層のトランザクション内で行い、`version` による楽観ロックで同時更新を検知する。
- 設定値を変更しても、既存の支出明細の `deductible_amount` は自動更新しない。必要な場合は、対象ユーザー・対象期間を明確にした再計算処理を別途実行する。

