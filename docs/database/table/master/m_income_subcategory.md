## テーブル定義書

本テーブル定義書は、収益サブカテゴリを管理する `m_income_subcategory` テーブルの構造を定義する。

収益カテゴリに紐づく詳細分類を管理し、`t_income` から参照される。

## テーブル名

### m_income_subcategory

| No | 論理名        | 物理名                   | 型            | PK | NN | UQ | デフォルト                                         | 説明                                                |
|----|------------|-----------------------|--------------|----|----|----|-----------------------------------------------|---------------------------------------------------|
| 1  | 収益サブカテゴリID | income_subcategory_id | BIGINT       | ○  | ○  | ○  | AUTO_INCREMENT                                | 収益サブカテゴリの一意識別子。                                   |
| 2  | 収益カテゴリID   | income_category_id    | BIGINT       |    | ○  |    |                                               | `m_income_category.income_category_id` を参照する外部キー。 |
| 3  | サブカテゴリ名    | subcategory_name      | VARCHAR(100) |    | ○  |    |                                               | サブカテゴリ名。例: 商品販売, 業務委託報酬。                          |
| 4  | 表示順        | display_order         | INT          |    | ○  |    | 0                                             | 画面表示順。                                            |
| 5  | ステータス      | status                | VARCHAR(20)  |    | ○  |    | ACTIVE                                        | 状態。例: ACTIVE, INACTIVE。                           |
| 6  | 登録日時       | created_at            | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                                         |
| 7  | 更新日時       | updated_at            | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                                         |
| 8  | バージョン      | version               | BIGINT       |    | ○  |    | 0                                             | 楽観ロック用バージョン。                                      |

## 制約

| 種別          | 対象                                   | 制約名                                   | 内容                                            |
|-------------|--------------------------------------|---------------------------------------|-----------------------------------------------|
| PRIMARY KEY | income_subcategory_id                | pk_m_income_subcategory               | 収益サブカテゴリIDを主キーとする。                            |
| FOREIGN KEY | income_category_id                   | fk_m_income_subcategory_category      | `m_income_category.income_category_id` を参照する。 |
| UNIQUE      | income_category_id, subcategory_name | uq_m_income_subcategory_category_name | 同一収益カテゴリ内で同じサブカテゴリ名を重複させない。                   |

## 推奨インデックス

| インデックス名                                        | 対象カラム                                     | 用途                      |
|------------------------------------------------|-------------------------------------------|-------------------------|
| idx_m_income_subcategory_category_status_order | income_category_id, status, display_order | 収益カテゴリ単位の有効サブカテゴリ表示順取得。 |

## 初期マスタデータ

### 事業収入（income_category_id=1）のサブカテゴリ

| income_subcategory_id | income_category_id | subcategory_name | display_order |
|-----------------------|--------------------|------------------|---------------|
| 1                     | 1                  | 商品売上             | 1             |
| 2                     | 1                  | サービス売上           | 2             |
| 3                     | 1                  | 制作売上             | 3             |
| 4                     | 1                  | 開発売上(業務委託)       | 4             |
| 5                     | 1                  | 開発売上(請負)         | 5             |
| 6                     | 1                  | コンサル売上           | 6             |
| 7                     | 1                  | 広告収益             | 7             |
| 8                     | 1                  | アフィリエイト          | 8             |
| 9                     | 1                  | YouTube収益        | 9             |
| 10                    | 1                  | SNS収益            | 10            |
| 11                    | 1                  | ライセンス収益          | 11            |
| 12                    | 1                  | サブスク収益           | 12            |
| 13                    | 1                  | システム利用料          | 13            |
| 14                    | 1                  | 手数料収益            | 14            |
| 15                    | 1                  | イベント売上           | 15            |
| 16                    | 1                  | その他事業収益          | 16            |

### 金融・投資収益（income_category_id=2）のサブカテゴリ

| income_subcategory_id | income_category_id | subcategory_name | display_order |
|-----------------------|--------------------|------------------|---------------|
| 17                    | 2                  | 株式配当             | 1             |
| 18                    | 2                  | 売却益              | 2             |
| 19                    | 2                  | FX収益             | 3             |
| 20                    | 2                  | 仮想通貨利益           | 4             |
| 21                    | 2                  | ステーキング           | 5             |
| 22                    | 2                  | 投資信託収益           | 6             |
| 23                    | 2                  | 銀行利息             | 7             |
| 24                    | 2                  | 外貨利息             | 8             |
| 25                    | 2                  | その他金融・投資収益       | 9             |

### 一時収益（income_category_id=3）のサブカテゴリ

| income_subcategory_id | income_category_id | subcategory_name | display_order |
|-----------------------|--------------------|------------------|---------------|
| 26                    | 3                  | 不用品売却            | 1             |
| 27                    | 3                  | 臨時収入             | 2             |
| 28                    | 3                  | 保険金              | 3             |
| 29                    | 3                  | お祝い金             | 4             |
| 30                    | 3                  | 懸賞               | 5             |
| 31                    | 3                  | 返戻金              | 6             |
| 32                    | 3                  | その他一時収益          | 7             |

### 非課税・対象外収益（income_category_id=4）のサブカテゴリ

| income_subcategory_id | income_category_id | subcategory_name | display_order |
|---|---|---|---|
| 33 | 4 | 借入金 | 1 |
| 34 | 4 | 資本投入費 | 2 |
| 35 | 4 | 建て替え金 | 3 |
| 36 | 4 | 預かり金 | 4 |
| 37 | 4 | 振替 | 5 |
| 38 | 4 | 補助・給付金 | 6 |
| 39 | 4 | その他非課税・対象外収益 | 7 |

## 備考

- 初期実装では全ユーザー共通の固定マスタとして扱う。
- サブカテゴリを選択しない収益登録を許容する場合、`t_income.income_subcategory_id` は NULL 許容とする。
- 初期マスタデータは上記一覧をもとに INSERT 文で投入する。
- 各カテゴリのサブカテゴリは業務要件に応じて増減可能な設計とする。

