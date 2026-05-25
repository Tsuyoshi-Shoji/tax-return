## テーブル定義書

本テーブル定義書は、支払方法を管理する `m_payment_method` テーブルの構造を定義する。

## テーブル名

### m_payment_method

| No | 論理名    | 物理名                 | 型            | PK | NN | UQ | デフォルト                                         | 説明                           |
|----|--------|---------------------|--------------|----|----|----|-----------------------------------------------|------------------------------|
| 1  | 支払方法ID | payment_method_id   | BIGINT       | ○  | ○  | ○  | AUTO_INCREMENT                                | 支払方法の一意識別子。                  |
| 2  | 支払方法名  | payment_method_name | VARCHAR(100) |    | ○  | ○  |                                               | 支払方法名。例: 現金, クレジットカード, 口座振替。 |
| 3  | 表示順    | display_order       | INT          |    | ○  |    | 0                                             | 画面表示順。                       |
| 4  | ステータス  | status              | VARCHAR(20)  |    | ○  |    | ACTIVE                                        | 状態。例: ACTIVE, INACTIVE。      |
| 5  | 登録日時   | created_at          | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                    |
| 6  | 更新日時   | updated_at          | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                    |
| 7  | バージョン  | version             | BIGINT       |    | ○  |    | 0                                             | 楽観ロック用バージョン。                 |

## 制約

| 種別          | 対象                  | 制約名                      | 内容                |
|-------------|---------------------|--------------------------|-------------------|
| PRIMARY KEY | payment_method_id   | pk_m_payment_method      | 支払方法IDを主キーとする。    |
| UNIQUE      | payment_method_name | uq_m_payment_method_name | 同一支払方法名の重複を許可しない。 |

## 推奨インデックス

| インデックス名                           | 対象カラム                 | 用途             |
|-----------------------------------|-----------------------|----------------|
| idx_m_payment_method_status_order | status, display_order | 有効な支払方法の表示順取得。 |

## 備考

- 支払方法は `t_expense.payment_method_id` から参照される。
