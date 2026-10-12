# 画面設計書 - 損益詳細画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

損益一覧から選択した収益または支出の詳細情報を表示する画面。編集画面（収益登録/支出登録）への遷移、および論理削除操作が可能。削除は `window.confirm` による確認ダイアログを経て POST 送信する。

- **JSP ファイル**: `ProfitLoss/profit-loss-detail.jsp`
- **URL パス**: `/profit-loss/detail`
- **HTTP メソッド**: GET（表示）/ POST（削除: `/profit-loss/delete`）
- **リクエストパラメータ**: `recordType`（`income` / `expense`）、`id`

---

## 画面項目一覧

### ページタイトルセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ページタイトル | `page-title` | 表示 | ○ | 「損益詳細」の固定表示 |
| 説明文 | `page-description` | 表示 | ○ | 「一覧で選択した損益データの詳細を確認できます。」の固定表示 |

### 詳細情報セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 区分ラベル | `label-category` | 表示 | ○ | 「区分」の固定表示 |
| 区分表示 | `display-category` | ドロップダウン表示 | ○ | 区分を表示（編集不可） |
| 項目ラベル | `label-item` | 表示 | ○ | 「項目」の固定表示 |
| 項目表示 | `display-item` | ドロップダウン表示 | ○ | 項目を表示（編集不可） |
| 発生日ラベル | `label-date` | 表示 | ○ | 「発生日」の固定表示 |
| 発生日表示 | `display-date` | 日付表示 | ○ | 発生日を表示（編集不可） |
| 金額ラベル | `label-amount` | 表示 | ○ | 「金額」の固定表示 |
| 金額表示 | `display-amount` | 数値表示 | ○ | 金額を表示（右に「円」単位） |
| 支払方法ラベル | `label-payment-method` | 表示 | ○ | 「支払方法」の固定表示 |
| 支払方法表示 | `display-payment-method` | ドロップダウン表示 | ○ | 支払方法を表示（編集不可） |
| 申告対象外チェック | `checkbox-exclude-from-return` | チェックボックス表示 | - | 「この支出を確定申告対象外にする」（表示のみ） |

### 家事按分セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 家事按分見出し | `heading-home-apportionment` | 見出し | - | 「家事按分」の固定表示 |
| 家事按分適用表示 | `display-apportionment-applied` | チェックボックス表示 | - | 「家事按分を適用する」（チェック済みの場合のみ表示） |
| 按分率表示 | `display-apportionment-rate` | 数値表示 | - | 「家事按分率 [%]」 |
| 按分説明文 | `text-apportionment-description` | 表示 | - | 按分方法の説明文 |
| 経費対象金額表示 | `display-deductible-amount` | 数値表示 | - | 「経費対象金額 [金額]円」 |

### 詳細情報セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 詳細ラベル | `label-details` | 表示 | ○ | 「詳細」の固定表示 |
| 詳細表示 | `display-details` | テキスト表示 | ○ | 詳細情報をテキスト表示 |

### 操作ボタンセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 編集ボタン | `button-edit` | ボタン | ○ | 編集画面へ遷移 |
| 削除ボタン | `button-delete` | 送信ボタン | ○ | 論理削除を実行（確認ダイアログ付き） |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| 一覧へ戻るクリック | クリック | `/profit-loss` へ遷移 |
| 編集ボタンクリック | クリック | 収益/支出に応じた編集画面へ遷移 |
| 削除ボタンクリック（フォーム送信） | submit | `window.confirm` で確認ダイアログを表示 → OK 時 POST `/profit-loss/delete` → キャンセル時は送信を中断 |

---

## バリデーションチェック（フロント）仕様

| 項目 | チェック内容 | 説明 |
|------|------------|------|
| 削除確認 | `window.confirm` ダイアログ | 「このデータを削除します。よろしいですか？」キャンセル時は POST 送信を中断 |

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller（推定） | `ProfitLossController` | `org.example.controller` または `org.example.features.ProfitLoss.controller` |
| DTO（詳細表示） | `ProfitLossDetailView` | `org.example.features.ProfitLoss.dto` |
| Service | `ProfitLossService` | `org.example.features.ProfitLoss.service` |
| Service 実装 | `ProfitLossServiceImpl` | `org.example.features.ProfitLoss.service.impl` |
| Entity | `Income` | `org.example.entity` |
| Entity | `Expense` | `org.example.entity` |
| Repository | `IncomeRepository` | `org.example.repository` |
| Repository | `ExpenseRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*

