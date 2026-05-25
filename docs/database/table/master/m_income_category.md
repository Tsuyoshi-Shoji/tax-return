## テーブル定義書

本テーブル定義書は、収益カテゴリを管理する `m_income_category` テーブルの構造を定義する。

## テーブル名

### m_income_category

| No | 論理名      | 物理名                | 型            | PK | NN | UQ | デフォルト                                         | 説明                      |
|----|----------|--------------------|--------------|----|----|----|-----------------------------------------------|-------------------------|
| 1  | 収益カテゴリID | income_category_id | BIGINT       | ○  | ○  | ○  | AUTO_INCREMENT                                | 収益カテゴリの一意識別子。           |
| 2  | カテゴリ名    | category_name      | VARCHAR(100) |    | ○  | ○  |                                               | カテゴリ名。例: 売上, 雑収入。       |
| 3  | 表示順      | display_order      | INT          |    | ○  |    | 0                                             | 画面表示順。                  |
| 4  | ステータス    | status             | VARCHAR(20)  |    | ○  |    | ACTIVE                                        | 状態。例: ACTIVE, INACTIVE。 |
| 5  | 登録日時     | created_at         | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。               |
| 6  | 更新日時     | updated_at         | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。               |
| 7  | バージョン    | version            | BIGINT       |    | ○  |    | 0                                             | 楽観ロック用バージョン。            |

## 制約

| 種別          | 対象                 | 制約名                       | 内容                |
|-------------|--------------------|---------------------------|-------------------|
| PRIMARY KEY | income_category_id | pk_m_income_category      | 収益カテゴリIDを主キーとする。  |
| UNIQUE      | category_name      | uq_m_income_category_name | 同一カテゴリ名の重複を許可しない。 |

## 推奨インデックス

| インデックス名                            | 対象カラム                 | 用途            |
|------------------------------------|-----------------------|---------------|
| idx_m_income_category_status_order | status, display_order | 有効カテゴリの表示順取得。 |

## 初期マスタデータ

### 収益区分

| income_category_id | category_name | display_order |
|--------------------|---------------|---------------|
| 1                  | 事業収入          | 1             |
| 2                  | 金融・投資収益       | 2             |
| 3                  | 一時収益          | 3             |
| 4                  | 非課税・対象外収益     | 4             |

## 備考

- 初期実装では全ユーザー共通の固定マスタとして扱う。
- 将来的にユーザー別カテゴリを許可する場合は、`user_id` を追加し、`UNIQUE(user_id, category_name)` へ変更する。
- 各カテゴリに対応する具体的な項目は `m_income_subcategory` で定義する。

