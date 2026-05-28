# 画面設計書 - 損益レポート画面

[← インデックスへ戻る](index.md)

---

## 画面概要

年度を選択して、全データ版・事業収支版の月次損益テーブルおよび棒グラフを表示する。月ヘッダーリンクから損益詳細レポート画面へ遷移可能。年度サマリー（総収益・総支出・損益）も表示する。PDF 出力（ブラウザ印刷ダイアログ）機能を持つ。

- **JSP ファイル**: `Report/report.jsp`
- **URL パス**: `/report`
- **HTTP メソッド**: GET

---

## 画面項目一覧

### 年度・操作

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 表示年度セレクトボックス | セレクトボックス | `reportViewAll.selectableYears` から動的生成。変更時に GET 送信 |
| PDF 出力ボタン | ボタン | `window.print()` を実行 |

### 全データレポート

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 年度ラベル | 表示 | `reportViewAll.year` 年度 |
| 月別収益（1〜12月） | 表示 | `reportViewAll.monthlyRows[n].income` |
| 月別支出（1〜12月） | 表示 | `reportViewAll.monthlyRows[n].expense` |
| 月別損益（1〜12月） | 表示 | 収益 - 支出（JS で計算） |
| 年間合計 収益 | 表示 | 月別収益の合計（JS で計算） |
| 年間合計 支出 | 表示 | 月別支出の合計（JS で計算） |
| 年間合計 損益 | 表示 | 年間収益 - 年間支出（JS で計算） |
| 月リンク（1〜12月） | リンク | `/report/detail?year=...&month=...` へ遷移 |
| 当年度 総収益サマリー | 表示 | `reportViewAll.totalIncome` |
| 当年度 総支出サマリー | 表示 | `reportViewAll.totalExpense` |
| 当年度 損益サマリー | 表示 | `reportViewAll.profitAmount` |
| 月別損益棒グラフ | 表示 | CSS でレンダリング（青: 収益バー、赤: 支出バー） |

### 事業収支レポート

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 年度ラベル | 表示 | `reportViewBusiness.year` 年度 |
| 月別収益（1〜12月） | 表示 | `reportViewBusiness.monthlyRows[n].income` |
| 月別支出（1〜12月） | 表示 | `reportViewBusiness.monthlyRows[n].expense` |
| 月別損益（1〜12月） | 表示 | 収益 - 支出（JS で計算） |
| 年間合計（収益・支出・損益） | 表示 | 月別集計（JS で計算） |
| 月リンク（1〜12月） | リンク | `/report/detail?year=...&month=...` へ遷移 |
| 当年度 総収益サマリー | 表示 | `reportViewBusiness.totalIncome` |
| 当年度 総支出サマリー | 表示 | `reportViewBusiness.totalExpense` |
| 当年度 損益サマリー | 表示 | `reportViewBusiness.profitAmount` |
| 月別損益棒グラフ | 表示 | CSS でレンダリング（申告対象外除く） |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| 年度セレクト変更 | `yearSelect` change | `yearForm` を GET 送信（`/report?year=...`） |
| 月リンククリック | click | `/report/detail?year=...&month=...` へ遷移 |
| PDF 出力ボタンクリック | click | タイトルを「損益レポート_年度」に変更 → `window.print()` → タイトルを元に戻す |
| 棒グラフグループクリック | click | 対応する月の損益詳細レポートへ遷移（`<a>` タグで実装） |

---

## バリデーションチェック（フロント）仕様

なし（表示・集計専用画面）

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller（推定） | `ReportController` | `org.example.controller` または `org.example.features.Report.controller` |
| DTO（レポート表示） | `ReportView` | `org.example.features.Report.dto` |
| DTO（月次行） | `MonthlyAmountRow` | `org.example.features.Report.dto` |
| Service | `ReportService` | `org.example.features.Report.service` |
| Service 実装 | `ReportServiceImpl` | `org.example.features.Report.service.impl` |
| Entity | `Income` | `org.example.entity` |
| Entity | `Expense` | `org.example.entity` |
| Repository | `IncomeRepository` | `org.example.repository` |
| Repository | `ExpenseRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*

