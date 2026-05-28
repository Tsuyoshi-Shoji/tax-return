# 画面設計書 - 申告書作成画面

[← インデックスへ戻る](index.md)

---

## 画面概要

当年の収益・経費集計をもとに、申告準備に必要な基礎金額（総収益・経費対象金額・経費対象外支出・課税対象の概算利益）を確認する画面。確定申告書の自動作成機能は持たず、参考値の確認と次のアクションへの誘導が目的。

- **JSP ファイル**: `ReturnForm/return-form.jsp`
- **URL パス**: `/return-form`
- **HTTP メソッド**: GET

---

## 画面項目一覧

### 集計サマリー

| 項目名 | 種別 | モデル属性 |
|--------|------|-----------|
| 対象年度 | 表示（見出し） | `returnFormView.year` |
| 総収益 | 表示 | `returnFormView.totalIncome`（円） |
| 経費対象金額 | 表示 | `returnFormView.deductibleExpense`（円） |
| 経費対象外支出 | 表示 | `returnFormView.nonDeductibleExpense`（円） |
| 課税対象の概算利益 | 表示（強調） | `returnFormView.taxableProfit`（円） |

### 次のアクション

| 項目名 | 種別 | 説明 |
|--------|------|------|
| 次のアクション説明リスト | 表示 | 申告前チェック事項の案内文（3項目） |
| 収益登録へリンク | リンクボタン | `/income` へ遷移 |
| 支出登録へリンク | リンクボタン | `/expense` へ遷移 |
| 損益レポートへリンク | リンクボタン | `/report` へ遷移 |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| 収益登録へリンククリック | クリック | `GET /income` へ遷移 |
| 支出登録へリンククリック | クリック | `GET /expense` へ遷移 |
| 損益レポートへリンククリック | クリック | `GET /report` へ遷移 |

---

## バリデーションチェック（フロント）仕様

なし（表示専用画面）

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller（推定） | `ReturnFormController` | `org.example.controller` または `org.example.features.ReturnForm.controller` |
| DTO（申告書表示） | `ReturnFormView` | `org.example.features.ReturnForm.dto` |
| Service | `ReturnFormService` | `org.example.features.ReturnForm.service` |
| Service 実装 | `ReturnFormServiceImpl` | `org.example.features.ReturnForm.service.impl` |
| Entity | `Income` | `org.example.entity` |
| Entity | `Expense` | `org.example.entity` |
| Repository | `IncomeRepository` | `org.example.repository` |
| Repository | `ExpenseRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*

