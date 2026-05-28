# 画面設計書 - 損益詳細レポート画面

[← インデックスへ戻る](index.md)

---

## 画面概要

損益レポートの月リンクから遷移する月次詳細画面。全データ版・事業収支版の両方について、当月の収益・支出・損益サマリー、項目別累計テーブル、円グラフ（CSS `conic-gradient`）、発生日カレンダー（青ドット: 収益、赤ドット: 支出）を表示する。

- **JSP ファイル**: `Report/report-detail.jsp`
- **URL パス**: `/report/detail`
- **HTTP メソッド**: GET
- **リクエストパラメータ**: `year`（年）、`month`（月）

---

## 画面項目一覧

### 共通

| 項目名 | 種別 | 説明 |
|--------|------|------|
| 対象年月表示 | 表示 | `data.year` 年 `data.month` 月（ページサブタイトルに表示） |

### 全データ詳細レポートセクション

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 収益（月合計） | 表示 | `reportDetailViewAll.incomeBreakdown` の合計（JS で計算） |
| 支出（月合計） | 表示 | `reportDetailViewAll.expenseBreakdown` の合計（JS で計算）。支出金額は `t_expense.amount` を使用 |
| 損益（月合計） | 表示 | 収益 - 支出（JS で計算） |
| 収益 項目別累計テーブル | テーブル | `reportDetailViewAll.incomeBreakdown`（項目名・金額） |
| 収益割合 円グラフ | 表示 | `conic-gradient` CSS で描画（5色: 青系） |
| 支出 項目別累計テーブル | テーブル | `reportDetailViewAll.expenseBreakdown`（項目名・金額）。支出金額は `t_expense.amount` を使用 |
| 支出割合 円グラフ | 表示 | `conic-gradient` CSS で描画（5色: 赤系） |
| 発生日カレンダー | 表示 | `reportDetailViewAll.calendarEvents`（青ドット: 収益、赤ドット: 支出） |
| 年度レポートへ戻るリンク | リンク | `/report?year=${reportDetailViewAll.year}` へ遷移 |

### 事業収支詳細レポートセクション

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 収益（月合計） | 表示 | `reportDetailViewBusiness.incomeBreakdown` の合計（JS で計算） |
| 支出（月合計） | 表示 | `reportDetailViewBusiness.expenseBreakdown` の合計（JS で計算）。支出金額は `t_expense.deductible_amount` を使用 |
| 損益（月合計） | 表示 | 収益 - 支出（JS で計算） |
| 収益 項目別累計テーブル | テーブル | `reportDetailViewBusiness.incomeBreakdown`（項目名・金額） |
| 収益割合 円グラフ | 表示 | `conic-gradient` CSS で描画（5色: 青系） |
| 支出 項目別累計テーブル | テーブル | `reportDetailViewBusiness.expenseBreakdown`（項目名・金額）。支出金額は `t_expense.deductible_amount` を使用 |
| 支出割合 円グラフ | 表示 | `conic-gradient` CSS で描画（5色: 赤系） |
| 発生日カレンダー | 表示 | `reportDetailViewBusiness.calendarEvents`（青ドット: 収益、赤ドット: 支出） |
| 年度レポートへ戻るリンク | リンク | `/report?year=${reportDetailViewBusiness.year}` へ遷移 |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| 年度レポートへ戻るリンククリック | クリック | 対応する年度の損益レポート画面へ遷移 |

---

## バリデーションチェック（フロント）仕様

なし（表示専用画面）

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller（推定） | `ReportController` | `org.example.controller` または `org.example.features.Report.controller` |
| DTO（詳細レポート表示） | `ReportDetailView` | `org.example.features.Report.dto` |
| DTO（項目別金額行） | `ItemAmountRow` | `org.example.features.Report.dto` |
| DTO（カレンダーイベント） | `CalendarEvent` | `org.example.features.Report.dto` |
| Service | `ReportService` | `org.example.features.Report.service` |
| Service 実装 | `ReportServiceImpl` | `org.example.features.Report.service.impl` |
| Entity | `Income` | `org.example.entity` |
| Entity | `Expense` | `org.example.entity` |
| Repository | `IncomeRepository` | `org.example.repository` |
| Repository | `ExpenseRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*

