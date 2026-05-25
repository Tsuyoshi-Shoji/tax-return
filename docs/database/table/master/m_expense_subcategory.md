## テーブル定義書

本テーブル定義書は、支出サブカテゴリを管理する `m_expense_subcategory` テーブルの構造を定義する。

支出カテゴリに紐づく詳細分類を管理し、`t_expense` から参照される。

## テーブル名

### m_expense_subcategory

| No | 論理名        | 物理名                    | 型            | PK | NN | UQ | デフォルト                                         | 説明                                                  |
|----|------------|------------------------|--------------|----|----|----|-----------------------------------------------|-----------------------------------------------------|
| 1  | 支出サブカテゴリID | expense_subcategory_id | BIGINT       | ○  | ○  | ○  | AUTO_INCREMENT                                | 支出サブカテゴリの一意識別子。                                     |
| 2  | 支出カテゴリID   | expense_category_id    | BIGINT       |    | ○  |    |                                               | `m_expense_category.expense_category_id` を参照する外部キー。 |
| 3  | サブカテゴリ名    | subcategory_name       | VARCHAR(100) |    | ○  |    |                                               | サブカテゴリ名。例: 電車代, 書籍, 文房具。                            |
| 4  | 表示順        | display_order          | INT          |    | ○  |    | 0                                             | 画面表示順。                                              |
| 5  | ステータス      | status                 | VARCHAR(20)  |    | ○  |    | ACTIVE                                        | 状態。例: ACTIVE, INACTIVE。                             |
| 6  | 登録日時       | created_at             | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                                           |
| 7  | 更新日時       | updated_at             | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                                           |
| 8  | バージョン      | version                | BIGINT       |    | ○  |    | 0                                             | 楽観ロック用バージョン。                                        |

## 制約

| 種別          | 対象                                    | 制約名                                    | 内容                                              |
|-------------|---------------------------------------|----------------------------------------|-------------------------------------------------|
| PRIMARY KEY | expense_subcategory_id                | pk_m_expense_subcategory               | 支出サブカテゴリIDを主キーとする。                              |
| FOREIGN KEY | expense_category_id                   | fk_m_expense_subcategory_category      | `m_expense_category.expense_category_id` を参照する。 |
| UNIQUE      | expense_category_id, subcategory_name | uq_m_expense_subcategory_category_name | 同一支出カテゴリ内で同じサブカテゴリ名を重複させない。                     |

## 推奨インデックス

| インデックス名                                         | 対象カラム                                      | 用途                      |
|-------------------------------------------------|--------------------------------------------|-------------------------|
| idx_m_expense_subcategory_category_status_order | expense_category_id, status, display_order | 支出カテゴリ単位の有効サブカテゴリ表示順取得。 |

## 初期マスタデータ

### 経費（expense_category_id=1）のサブカテゴリ

| expense_subcategory_id | expense_category_id | subcategory_name | display_order |
|------------------------|---------------------|------------------|---------------|
| 1                      | 1                   | 通信・IT利用料         | 1             |
| 2                      | 1                   | 設備・機材            | 2             |
| 3                      | 1                   | 外注費              | 3             |
| 4                      | 1                   | 広告宣伝費            | 4             |
| 5                      | 1                   | 旅費交通費            | 5             |
| 6                      | 1                   | 会議費              | 6             |
| 7                      | 1                   | 接待交際費            | 7             |
| 8                      | 1                   | 地代家賃             | 8             |
| 9                      | 1                   | 水道光熱費            | 9             |
| 10                     | 1                   | スペース利用料          | 10            |
| 11                     | 1                   | 学習・教材費           | 11            |
| 12                     | 1                   | 手数料              | 12            |
| 13                     | 1                   | 保険（事業保険・賠償責任保険）  | 13            |
| 14                     | 1                   | 税務相談費            | 14            |
| 15                     | 1                   | 法務相談費            | 15            |
| 16                     | 1                   | 消耗品費             | 16            |
| 17                     | 1                   | 雑費               | 17            |

### 公的負担（expense_category_id=2）のサブカテゴリ

| expense_subcategory_id | expense_category_id | subcategory_name        | display_order |
|------------------------|---------------------|-------------------------|---------------|
| 18                     | 2                   | 所得税                     | 1             |
| 19                     | 2                   | 住民税                     | 2             |
| 20                     | 2                   | 個人事業税                   | 3             |
| 21                     | 2                   | 固定資産税                   | 4             |
| 22                     | 2                   | 自動車税                    | 5             |
| 23                     | 2                   | 消費税                     | 6             |
| 24                     | 2                   | 国民健康保険                  | 7             |
| 25                     | 2                   | 国民年金                    | 8             |
| 26                     | 2                   | 介護保険                    | 9             |
| 27                     | 2                   | 証明書発行手数料                | 10            |
| 28                     | 2                   | 行政手続料                   | 11            |
| 29                     | 2                   | 許可申請費                   | 12            |
| 30                     | 2                   | その他公的支出（罰金・延滞税、各種協会費など） | 13            |

### 私的利用（expense_category_id=3）のサブカテゴリ

| expense_subcategory_id | expense_category_id | subcategory_name  | display_order |
|------------------------|---------------------|-------------------|---------------|
| 31                     | 3                   | 生活費               | 1             |
| 32                     | 3                   | 趣味・娯楽             | 2             |
| 33                     | 3                   | 教育費               | 3             |
| 34                     | 3                   | 医療費               | 4             |
| 35                     | 3                   | 交通費               | 5             |
| 36                     | 3                   | 交際費               | 6             |
| 37                     | 3                   | その他私費(雑費・貯金・投資など) | 7             |

## 備考

- 初期実装では全ユーザー共通の固定マスタとして扱う。
- サブカテゴリを選択しない支出登録を許容する場合、`t_expense.expense_subcategory_id` は NULL 許容とする。
- 初期マスタデータは上記一覧をもとに INSERT 文で投入する。
- 各カテゴリのサブカテゴリは業務要件に応じて増減可能な設計とする。

