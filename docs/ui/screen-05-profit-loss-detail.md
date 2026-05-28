# 画面設計書 - 損益詳細画面

[← インデックスへ戻る](index.md)

---

## 画面概要

損益一覧から選択した収益または支出の詳細情報を表示する画面。編集画面（収益登録/支出登録）への遷移、および論理削除操作が可能。削除は `window.confirm` による確認ダイアログを経て POST 送信する。

- **JSP ファイル**: `ProfitLoss/profit-loss-detail.jsp`
- **URL パス**: `/profit-loss/detail`
- **HTTP メソッド**: GET（表示）/ POST（削除: `/profit-loss/delete`）
- **リクエストパラメータ**: `recordType`（`income` / `expense`）、`id`

---

## 画面項目一覧

### 詳細情報表示

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 種別 | 表示 | `detail.recordType`（`income` → 収益、`expense` → 支出） |
| 発生日 | 表示 | `detail.date` |
| 区分 | 表示 | `detail.type` |
| 項目 | 表示 | `detail.item` |
| 金額 | 表示 | `detail.amount`（円表示） |
| 詳細 | 表示 | `detail.description` |

### 操作ボタン

| 項目名 | 種別 | 説明 |
|--------|------|------|
| 一覧へ戻るボタン | リンクボタン | `/profit-loss` へ遷移 |
| 編集ボタン | リンクボタン | 収益: `/income?editId=${detail.id}&returnTo=detail`、支出: `/expense?editId=${detail.id}&returnTo=detail` |
| 削除ボタン | POSTフォーム送信ボタン | `POST /profit-loss/delete`（`recordType` / `id` を hidden で送信） |

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

