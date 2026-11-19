# 詳細設計書 - クレジットカード引き落とし仕訳

[← 確定申告画面仕様へ戻る](screen-03-tax-return_spec.md)

---

## 1. 目的

支出登録で支払方法がクレジットカードの取引について、利用時点の未払計上だけでなく、引き落とし時点の消込仕訳を帳簿へ記録する。

---

## 2. 背景と課題

- 現行はクレカ利用時に `借方: 経費 / 貸方: 未払金` を生成する。
- 仕訳帳確認（`/tax-return/journal/confirm`）は確認状態更新のみで、引き落とし仕訳は生成しない。
- このため、未払金の消込履歴を日次の仕訳として追跡しにくい。

---

## 3. スコープ

### 3.1 対象

1. 未決済クレカ取引の検索・選択
2. 引き落とし登録（全額/一部）
3. 引き落とし仕訳の自動生成
4. 取引の消込状態更新（`UNSETTLED` / `PARTIALLY_SETTLED` / `SETTLED`）

### 3.2 対象外

1. カード会社明細の自動連携（API）
2. 利息・分割手数料の自動判定
3. 税務適否の自動判定（利用者確認前提）

---

## 4. 業務ルール

| No | ルール |
|---|---|
| CR-01 | 対象は「支出登録」かつ `payment_method=card` の取引のみ |
| CR-02 | 締め済み年度では引き落とし登録不可 |
| CR-03 | 引き落とし額は対象取引の未決済残高を超えてはならない |
| CR-04 | 一部消込時は `PARTIALLY_SETTLED`、全額消込時は `SETTLED` |
| CR-05 | 仕訳は必ず借貸一致。保存はトランザクションで原子的に処理 |
| CR-06 | 同一リクエストの二重送信は冪等性キーで防止 |

---

## 5. 画面設計

### 5.1 画面配置

- 確定申告画面 (`/tax-return`) の「仕訳帳」セクション配下に「カード引き落とし」サブセクションを追加する。
- 一覧・登録UIが複雑になる場合は専用画面 `/tax-return/card-settlement` へ分離する。

### 5.2 入力項目

| 項目 | name | 必須 | 説明 |
|---|---|---|---|
| 対象年度 | `year` | ○ | 表示年度と一致 |
| 引き落とし日 | `settlementDate` | ○ | 対象年度内の日付 |
| 引き落とし口座 | `accountCode` | ○ | 既定: 普通預金 |
| 対象取引 | `rows[*].sourceTransactionId` | ○ | 未決済クレカ取引 |
| 消込金額 | `rows[*].settledAmount` | ○ | 1円以上、未決済残高以下 |
| 手数料 | `feeAmount` | - | 必要時のみ |
| メモ | `memo` | - | 255文字以内 |
| CSRFトークン | `csrfToken` | ○ | 既存方針に従う |
| 冪等性キー | `idempotencyKey` | ○ | 二重送信防止 |

---

## 6. API設計

| API | 用途 |
|---|---|
| `GET /tax-return/card-settlement` | 未決済クレカ取引一覧の表示 |
| `POST /tax-return/card-settlement` | 引き落とし登録（仕訳生成・消込更新） |

### 6.1 POST リクエスト仕様

- 未知パラメータ拒否
- `rows` は1件以上
- `rows[*]` の `sourceTransactionId` は所有者・年度一致を必須検証

### 6.2 POST 成功時処理

1. 引き落としイベントの保存
2. 取引消込状態の更新
3. `journal_entry` / `journal_line` の生成
4. 仕訳確認状態は `draft` で作成

### 6.3 引き落とし日（支払日）の修正（2026-11-20）

- `POST /tax-return/card-settlement/date?year=`：`settlementTransactionId`、`currentDate`（表示時の引き落とし日）、`settlementDate`（修正後）、`csrfToken`。ログイン済み所有者のみ、CSRF必須。
- 確定申告画面の「登録済みの引き落とし（支払日の修正）」一覧から実行する。締め済み年度は表示のみで修正不可。
- 年度→引き落とし取引の順にロックし、所有者・年度・`CARD_SETTLEMENT`仕訳との対応を検証する。対象なしは404。
- 修正後の日付は対象年度内のみ。`currentDate`が現在値と異なる場合は他操作による変更とみなし拒否する。同日への変更は何もしない。
- 引き落とし取引の`transaction_date`と仕訳の`entry_date`を同時に更新し、仕訳を`draft`（未確認）へ戻す。金額・対象取引・消込イベントは変更しない（誤りは元の収支を修正する）。
- `audit.card-settlement`に利用者ID・年度・取引ID・結果のみ記録する。

---

## 7. データ設計

### 7.1 新規テーブル

`transaction_settlement_event`

| カラム | 型 | NULL | 説明 |
|---|---|---|---|
| id | BIGINT | NO | PK |
| fiscal_year_id | BIGINT | NO | 対象年度 |
| source_transaction_id | BIGINT | NO | 元のクレカ支出取引 |
| settlement_transaction_id | BIGINT | NO | 引き落とし取引（会計取引） |
| settled_amount | DECIMAL(15,2) | NO | 消込金額 |
| fee_amount | DECIMAL(15,2) | NO | 手数料（0許容） |
| idempotency_key | VARCHAR(64) | NO | 利用者単位の重複防止キー |
| created_at | DATETIME | NO | 作成日時 |
| updated_at | DATETIME | NO | 更新日時 |

### 7.2 制約

1. `settled_amount > 0`
2. `fee_amount >= 0`
3. `UNIQUE (fiscal_year_id, idempotency_key)`
4. FK: `fiscal_year_id`, `source_transaction_id`, `settlement_transaction_id`

---

## 8. 仕訳生成ルール

### 8.1 基本仕訳

- 引き落とし登録時（手数料なし）  
  - 借方: 未払金  
  - 貸方: 普通預金

### 8.2 手数料あり

- 借方: 未払金（消込額）
- 借方: 支払手数料（手数料）
- 貸方: 普通預金（合計）

### 8.3 一部消込

- 消込額分のみ `未払金` を減額
- 残高がある場合は `PARTIALLY_SETTLED`

---

## 9. アプリケーション設計

### 9.1 追加コンポーネント

| 種別 | クラス案 | 役割 |
|---|---|---|
| Controller | `CardSettlementController` | 画面表示/登録受け付け |
| Service | `CardSettlementService` | 引き落とし登録と仕訳生成 |
| Entity | `TransactionSettlementEvent` | 引き落としイベント |
| Repository | `TransactionSettlementEventRepository` | 永続化 |
| DTO/Form | `CardSettlementForm` | 入力バリデーション |

### 9.2 主処理シーケンス（POST）

1. 認証・CSRF・所有権・年度状態を検証
2. 冪等性キーを検証（重複なら再実行しない）
3. 対象取引を `FOR UPDATE` 相当でロック
4. 未決済残高を計算し、消込可能額を検証
5. `transaction_settlement_event` 保存
6. `accounting_transaction`（引き落とし取引）保存
7. `journal_entry/journal_line` 保存（`status=draft`）
8. `settlement_status` と `settlement_reference_id` を更新
9. 監査ログ出力（機密情報は除外）

---

## 10. セキュリティ/監査

1. user_id / tenant_id は認証コンテキスト由来のみ採用
2. リクエスト内の年度ID・取引IDを認可根拠にしない
3. 機密レスポンスは `no-store`
4. 監査ログは利用者ID、年度、対象件数、結果、イベントIDを記録（トークン・口座番号等は記録しない）

---

## 11. 例外・エラー仕様

| 条件 | HTTP | メッセージ |
|---|---|---|
| 締め済み年度 | 409 | 締め済み年度は登録できません |
| 対象取引なし/他人データ | 404 | 対象取引が見つかりません |
| 消込超過 | 409 | 消込金額が未決済残高を超えています |
| 不正入力 | 400 | 入力内容を確認してください |
| CSRF不正 | 403 | 不正な操作です |

---

## 12. マイグレーション計画

1. Flywayで `transaction_settlement_event` を追加
2. 既存データ移行は不要（新規イベントのみ対象）
3. 既存 `transaction_settlement`（決算整理配分）は用途を維持し、混在させない

---

## 13. テスト設計（受入観点）

1. クレカ取引を全額消込すると `SETTLED` になる
2. 一部消込で `PARTIALLY_SETTLED` になる
3. 複数回消込で合計超過が拒否される
4. 手数料ありで3行仕訳（未払金/手数料/預金）が生成される
5. 冪等性キー重複で二重仕訳が生成されない
6. 締め済み年度で登録拒否される
7. 他ユーザー取引ID指定が拒否される

---

*作成日: 2026-11-19*

