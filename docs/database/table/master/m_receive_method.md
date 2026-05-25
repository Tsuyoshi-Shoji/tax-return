## テーブル定義書

本テーブル定義書は、受取方法を管理する `m_receive_method` テーブルの構造を定義する。

## テーブル名

### m_receive_method

| No | 論理名    | 物理名                 | 型            | PK | NN | UQ | デフォルト                                         | 説明                        |
|----|--------|---------------------|--------------|----|----|----|-----------------------------------------------|---------------------------|
| 1  | 受取方法ID | receive_method_id   | BIGINT       | ○  | ○  | ○  | AUTO_INCREMENT                                | 受取方法の一意識別子。               |
| 2  | 受取方法名  | receive_method_name | VARCHAR(100) |    | ○  | ○  |                                               | 受取方法名。例: 現金, 銀行振込, 電子マネー。 |
| 3  | 表示順    | display_order       | INT          |    | ○  |    | 0                                             | 画面表示順。                    |
| 4  | ステータス  | status              | VARCHAR(20)  |    | ○  |    | ACTIVE                                        | 状態。例: ACTIVE, INACTIVE。   |
| 5  | 登録日時   | created_at          | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP                             | レコード作成日時。                 |
| 6  | 更新日時   | updated_at          | DATETIME     |    | ○  |    | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | レコード更新日時。                 |
| 7  | バージョン  | version             | BIGINT       |    | ○  |    | 0                                             | 楽観ロック用バージョン。              |

## 制約

| 種別          | 対象                  | 制約名                      | 内容                |
|-------------|---------------------|--------------------------|-------------------|
| PRIMARY KEY | receive_method_id   | pk_m_receive_method      | 受取方法IDを主キーとする。    |
| UNIQUE      | receive_method_name | uq_m_receive_method_name | 同一受取方法名の重複を許可しない。 |

## 推奨インデックス

| インデックス名                           | 対象カラム                 | 用途             |
|-----------------------------------|-----------------------|----------------|
| idx_m_receive_method_status_order | status, display_order | 有効な受取方法の表示順取得。 |

## 備考

- 受取方法は `t_income.receive_method_id` から参照される。

