# 画面設計書 - ホーム画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

ログイン後のトップ画面。収益・支出のクイックアクセス、最近の取引履歴、収支サマリー（当年度・当月の全データ版・事業収支版）、確定申告期限カウントダウン、その他機能へのナビゲーションを表示する。

- **JSP ファイル**: `Home/home.jsp`
- **URL パス**: `/`
- **HTTP メソッド**: GET
- **主要参照テーブル**: `app_user`, `fiscal_year`, `income_entry`, `expense_entry`, `accounting_transaction`

---

## 画面項目一覧

### ヘッダーセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| アプリロゴ | `logo` | 表示 | ○ | 「マネタックス」の固定表示 |
| サブタイトル | `subtitle` | 表示 | ○ | 「確定申告・資産管理」の固定表示 |
| 通知アイコン | `icon-notification` | ボタン | ○ | 通知機能へのアクセス |
| ユーザーメニュー | `menu-user` | ドロップダウン | ○ | ユーザー名表示とプロフィール・ログアウト機能 |

### サイドナビゲーション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ホームリンク | `nav-home` | リンク | ○ | `/` へ遷移 |
| 確定申告リンク | `nav-return` | リンク | ○ | 確定申告関連機能へ遷移 |
| 収益登録リンク | `nav-income` | リンク | ○ | `/income` へ遷移 |
| 支出登録リンク | `nav-expense` | リンク | ○ | `/expense` へ遷移 |
| 損益一覧リンク | `nav-profit-loss` | リンク | ○ | `/profit-loss` へ遷移 |
| レポートリンク | `nav-report` | リンク | ○ | `/report` へ遷移 |
| 各種設定リンク | `nav-settings` | リンク | ○ | `/settings` へ遷移 |

### メインコンテンツ - ウェルカムセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ウェルカムメッセージ | `welcome-message` | 表示 | ○ | 「おかえりなさい、[ユーザー名]さん」 |
| 説明文 | `welcome-description` | 表示 | ○ | 「確定申告の準備状況や資産のサマリーを確認できます。」 |
| 対象年度セレクト | `select-fiscal-year` | セレクトボックス | ○ | 年度選択（例: 2024年（令和6年）） |

### サマリーカード

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 総資産額表示 | `display-total-assets` | 表示 | ○ | 「総資産額 [金額]円」 |
| 前月比表示 | `display-prev-month-diff` | 表示 | ○ | 前月との増減を表示 |
| 当年度総損益表示 | `display-annual-profit-loss` | 表示 | ○ | 「当年度の総損益 [金額]円」 |
| 当年度収益表示 | `display-annual-income` | 表示 | ○ | 「収益 [金額]円」 |
| 当月収益表示 | `display-monthly-income` | 表示 | ○ | 「今月の収益 [金額]円」 |
| 当年度支出表示 | `display-annual-expense` | 表示 | ○ | 「支出 [金額]円」 |
| 当月支出表示 | `display-monthly-expense` | 表示 | ○ | 「今月の支出 [金額]円」 |

### 収支内訳セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 当月収支内訳グラフ | `chart-monthly-breakdown` | グラフ表示 | ○ | 円グラフで当月の収支項目別内訳を表示 |
| 収支内訳テーブル | `table-breakdown-items` | テーブル表示 | ○ | 項目と金額の一覧表示 |
| 詳細を見るリンク | `link-view-details` | リンク | ○ | 詳細ページへ遷移 |

### 確定申告期限セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| カウントダウン表示 | `display-days-until-deadline` | 表示 | ○ | 「あと[日数]日」の大型表示 |
| 提出期限表示 | `display-deadline-date` | 表示 | ○ | 「提出期限：[年]年[月]月[日]日（曜日）」 |
| 注記メッセージ | `message-deadline-note` | 表示 | ○ | 期限に関する注記（条件付き表示） |

### 情報セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 情報テーブル | `table-information` | テーブル表示 | ○ | 日付・タイプ（メモ/担当/相談など）・内容を表示 |
| 一覧を見るリンク | `link-view-all-info` | リンク | ○ | 情報一覧へ遷移 |

### 最近の取引セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 最近の取引テーブル | `table-recent-transactions` | テーブル表示 | ○ | 日付・区分・項目・詳細・金額を表示 |
| 取引一覧を見るリンク | `link-view-all-transactions` | リンク | ○ | `/profit-loss` へ遷移 |

---

## イベント一覧

| イベント | 操作 | 処理内容 |
|----------|------|---------|
| クイックアクセス - 収益登録クリック | クリック | `GET /income` へ遷移 |
| クイックアクセス - 支出登録クリック | クリック | `GET /expense` へ遷移 |
| 損益一覧リンククリック | クリック | `GET /profit-loss` へ遷移 |
| 損益レポートリンククリック | クリック | `GET /report` へ遷移 |
| 申告書作成リンククリック | クリック | `GET /return-form` へ遷移 |
| 設定リンククリック | クリック | `GET /settings` へ遷移 |
| 取引一覧を見るリンククリック | クリック | `GET /profit-loss` へ遷移 |

---

## API仕様

### `GET /`

- 目的: ホーム画面表示
- レスポンス: `200 OK`（JSPレンダリング）
- 必須条件: 認証済みユーザーのみ

### 画面表示用データ取得

- 目的: 年度選択、サマリー、最近の取引表示に必要なデータを取得
- 実装形態: サーバーサイドレンダリング時に取得、または内部APIで取得
- 異常時レスポンス（内部API採用時の想定）
  - 未認証: `401 Unauthorized`
  - 他ユーザー年度参照: `403 Forbidden`
  - 年度不正: `400 Bad Request`
  - システムエラー: `500 Internal Server Error`

---

## バックエンド検証仕様

- ウェルカム表示名は `app_user.display_name` から取得する。
- 年度選択は `fiscal_year` の対象ユーザー所有データのみに限定する。
- サマリー集計は対象年度（`fiscal_year.period_start`〜`period_end`）内データのみ対象にする。
- 収益集計は `income_entry.exclude_from_return=false` のみ対象にする。
- 支出集計は `expense_entry.exclude_from_return=false` かつ `deductible_amount` を優先対象にする。
- 最近の取引は `accounting_transaction.status='finalized'` を優先表示対象にする。

---

## セッション管理仕様

- ホーム画面は認証セッション必須とし、未認証アクセスはログイン画面へリダイレクトする。
- ユーザー切替時は前ユーザーのセッション情報を破棄し、再認証を要求する。
- 年度選択状態をセッション保持する場合でも、毎リクエストで所有者/権限を再検証する。

---

## 監査・ログ

- ホーム画面表示および年度切替イベントを監査ログ対象とする。
- 記録項目: 実行日時、ユーザーID、対象年度ID、処理結果（成功/失敗）。
- ログに機密データ本文、セッショントークン、Cookie値を出力しない。
- ログ要件は `docs/sequrity/security-test-plan.md` の `T-016` に準拠する。

---

## バリデーションチェック（フロント）仕様

なし（表示専用画面）

---

## バリデーションチェック（バックエンド）仕様

| 項目 | チェック内容 | エラー応答 |
|------|------------|----------|
| fiscalYearId | 未指定/形式不正 | `400 Bad Request` |
| fiscalYearId | 他ユーザー所有年度の指定 | `403 Forbidden` |
| 画面アクセス | 未認証 | `401 Unauthorized` |

---

## DB関連注記

- 表示名: `app_user.display_name` を参照する。
- 年度選択: `fiscal_year`（`business_profile_id`, `tax_year`, `status`）を参照する。
- 収益集計: `income_entry`（`amount`, `exclude_from_return`, `transaction_date`）を参照する。
- 支出集計: `expense_entry`（`deductible_amount`, `exclude_from_return`, `transaction_date`）を参照する。
- 最近の取引: `accounting_transaction`（`transaction_date`, `transaction_type`, `summary`, `total_amount`, `status`）を参照する。

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller | `HomeController` | `org.example.controller` |
| Service | `HomeService` | `org.example.features.Home.service` |
| Service 実装 | `HomeServiceImpl` | `org.example.features.Home.service.impl` |
| Entity | `AppUser` | `org.example.entity` |
| Entity | `FiscalYear` | `org.example.entity` |
| Entity | `IncomeEntry` | `org.example.entity` |
| Entity | `ExpenseEntry` | `org.example.entity` |
| Entity | `AccountingTransaction` | `org.example.entity` |
| DTO | `HomeSummaryDto` | `org.example.features.home.dto` |

---

## 実装上の注意

- `docs/architecture/coding-guidelines.md` に従い、Controllerで業務ロジックを持たずServiceへ委譲する。
- 金額集計は `BigDecimal` を使用し、丸め規則を明示する。
- 年度境界の抽出条件は `transaction_date` と `fiscal_year.period_start/period_end` で統一する。
- 一覧取得クエリには必ずユーザー/テナント境界条件を含める。
- ログ出力は `System.out.println` を使わず、SLF4Jを利用する。

---

*作成日: 2026-05-26*

