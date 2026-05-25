## テーブル定義書

本テーブル定義書は、支出カテゴリを管理する `m_expense_category` テーブルの構造を定義する。

## テーブル名

### m_expense_category

| No | 論理名      | 物理名                 | 型            | PK | NN | UQ | デフォルト                                         | 説明                                |
|----|----------|---------------------|--------------|----|----|----|-----------------------------------------------|-----------------------------------|
| 1  | 支出カテゴリID | expense_category_id | BIGINT       | ○  | ○  | ○  | AUTO_INCREMENT                                | 支出カテゴリの一意識別子。                     |
| 2  | カテゴリ名    | category_name       | VARCHAR(100) |    | ○  | ○  |                                               | カテゴリ名。例: 旅費交通費, 消耗品費。             |
| 3  | 支出区分     | expense_type        | VARCHAR(30)  |    | ○  |    | BUSINESS                                      | 支出区分。例: BUSINESS, PRIVATE, MIXED。 |
| 4  | 経費対象フラグ  | tax_deductible_flag | BOOLEAN      |    | ○  |    | TRUE                                          | 税務上の経費対象カテゴリかを示す。                 |
| 5  | 表示順      | display_order       | INT          |    | ○  |    | 0                                             | 画面表示順。                            |
| 6  | ステータス    | status              | VARCHAR(20)  |    | ○  |    | ACTIVE                                        | 状態。例: ACTIVE, INACTIVE。           |
| 7  | 登録日時     | created_at          | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                         |
| 8  | 更新日時     | updated_at          | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                         |
| 9  | バージョン    | version             | BIGINT       |    | ○  |    | 0                                             | 楽観ロック用バージョン。                      |

## 制約

| 種別          | 対象                  | 制約名                        | 内容                                                  |
|-------------|---------------------|----------------------------|-----------------------------------------------------|
| PRIMARY KEY | expense_category_id | pk_m_expense_category      | 支出カテゴリIDを主キーとする。                                    |
| UNIQUE      | category_name       | uq_m_expense_category_name | 同一カテゴリ名の重複を許可しない。                                   |
| CHECK       | expense_type        | ck_m_expense_category_type | `BUSINESS`（経費）、`PUBLIC`（公的負担）、`PRIVATE`（私的利用）のいずれか。 |

## 推奨インデックス

| インデックス名                             | 対象カラム                 | 用途            |
|-------------------------------------|-----------------------|---------------|
| idx_m_expense_category_status_order | status, display_order | 有効カテゴリの表示順取得。 |
| idx_m_expense_category_type         | expense_type          | 支出区分別の絞り込み。   |

## 初期マスタデータ

### 支出区分

| expense_category_id | category_name | expense_type | tax_deductible_flag | display_order |
|---------------------|---------------|--------------|---------------------|---------------|
| 1                   | 経費            | BUSINESS     | TRUE                | 1             |
| 2                   | 公的負担          | PUBLIC       | FALSE               | 2             |
| 3                   | 私的利用          | PRIVATE      | FALSE               | 3             |

## 備考

- 初期実装では全ユーザー共通の固定マスタとして扱う。
- `expense_type` は `BUSINESS`、`PUBLIC`、`PRIVATE` の3種類で支出区分を管理する。
  - `BUSINESS`: 事業経費（通常、家事按分対象）
  - `PUBLIC`: 公的負担（税金・保険など、家事按分対象外）
  - `PRIVATE`: 私的利用（家事按分可能な項目）
- カテゴリ単位の `tax_deductible_flag` は経費対象判定に使用し、家事按分率はユーザー設定 `m_user_setting.default_business_use_ratio` の固定値を使用する。
- 各カテゴリに対応する具体的な項目は `m_expense_subcategory` で定義する。
