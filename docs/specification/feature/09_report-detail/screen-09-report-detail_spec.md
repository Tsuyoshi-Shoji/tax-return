# 画面設計書 - 損益詳細レポート画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

損益レポートの月リンクから遷移する月次詳細画面。全データ版・事業収支版の両方について、当月の収益・支出・損益サマリー、項目別累計テーブル、円グラフ（CSS `conic-gradient`）、発生日カレンダー（青ドット: 収益、赤ドット: 支出）を表示する。

- **JSP ファイル**: `Report/report-detail.jsp`
- **URL パス**: `/report/detail`
- **HTTP メソッド**: GET
- **リクエストパラメータ**: `year`（年）、`month`（月）

---

## 画面項目一覧

### ページタイトルセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ページタイトル | `page-title` | 表示 | ○ | 「レポート詳細」の固定表示 |
| 説明文 | `page-description` | 表示 | ○ | 「選択した月の収支・損益の詳細を確認できます。各項目の内訳や発生した取引をカレンダーで確認できます。」の固定表示 |

### 月選択セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 月選択ラベル | `label-month-select` | 表示 | ○ | 「選択月」の固定表示 |
| 月選択ドロップダウン | `month-select` | セレクトボックス | ○ | 「YYYY年MM月」形式で表示、変更時にGET送信 |
| 前月ボタン | `button-prev-month` | ボタン | ○ | 前月を表示（リンク） |
| 次月ボタン | `button-next-month` | ボタン | ○ | 次月を表示（リンク） |

### 全データレポートセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-all-data-detail-report` | 見出し | ○ | 「全データレポート（申告対象外を含む）」 |
| 選択月累計収益 | `display-monthly-income-all` | 数値表示 | ○ | 金額を右寄せで表示 |
| 選択月累計支出 | `display-monthly-expense-all` | 数値表示 | ○ | 金額を右寄せで表示 |
| 選択月損益 | `display-monthly-profit-all` | 数値表示 | ○ | 金額を右寄せで表示 |
| 項目別累計収益テーブル | `table-income-breakdown-all-detail` | テーブル | - | 収益項目・金額・割合を表示 |
| 収益割合円グラフ | `chart-income-pie-all-detail` | グラフ | - | 収益項目の割合を円グラフで表示 |
| 項目別累計支出テーブル | `table-expense-breakdown-all-detail` | テーブル | - | 支出項目・金額・割合を表示 |
| 支出割合円グラフ | `chart-expense-pie-all-detail` | グラフ | - | 支出項目の割合を円グラフで表示 |

### 事業収支レポートセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-business-detail-report` | 見出し | ○ | 「事業収支レポート（申告対象のみ）」 |
| 選択月累計収益 | `display-monthly-income-business` | 数値表示 | ○ | 金額を右寄せで表示 |
| 選択月累計支出 | `display-monthly-expense-business` | 数値表示 | ○ | 金額を右寄せで表示 |
| 選択月損益 | `display-monthly-profit-business` | 数値表示 | ○ | 金額を右寄せで表示 |
| 項目別累計収益テーブル | `table-income-breakdown-business-detail` | テーブル | - | 収益項目・金額・割合を表示（申告対象のみ） |
| 収益割合円グラフ | `chart-income-pie-business-detail` | グラフ | - | 収益項目の割合を円グラフで表示（申告対象のみ） |
| 項目別累計支出テーブル | `table-expense-breakdown-business-detail` | テーブル | - | 支出項目・金額・割合を表示（申告対象のみ） |
| 支出割合円グラフ | `chart-expense-pie-business-detail` | グラフ | - | 支出項目の割合を円グラフで表示（申告対象のみ） |

### 発生カレンダーセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| カレンダー見出し | `heading-transaction-calendar` | 見出し | ○ | 「発生カレンダー」 |
| カレンダー | `calendar-display` | カレンダー | ○ | 月間カレンダーで取引を表示（青ドット: 収益、赤ドット: 支出、その他: 申告対象外） |

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

