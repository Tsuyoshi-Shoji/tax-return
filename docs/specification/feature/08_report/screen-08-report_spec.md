# 画面設計書 - 損益レポート画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

年度を選択して、全データ版・事業収支版の月次損益テーブルおよび棒グラフを表示する。月ヘッダーリンクから損益詳細レポート画面へ遷移可能。年度サマリー（総収益・総支出・損益）も表示する。PDF 出力（ブラウザ印刷ダイアログ）機能を持つ。

- **JSP ファイル**: `Report/report.jsp`
- **URL パス**: `/report`
- **HTTP メソッド**: GET

---

## 画面項目一覧

### ページタイトルセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ページタイトル | `page-title` | 表示 | ○ | 「レポート」の固定表示 |
| 説明文 | `page-description` | 表示 | ○ | 「選択した年度の月別の収支・損益の内訳を確認できます。申告対象の有無に応じた2種類のレポートを表示します。」の固定表示 |

### 操作セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 対象年度ラベル | `label-year-select` | 表示 | ○ | 「対象年度」の固定表示 |
| 対象年度セレクト | `year-select` | セレクトボックス | ○ | 年度選択（変更時にGET送信） |
| PDF出力ボタン | `button-pdf-output` | ボタン | ○ | ブラウザ印刷ダイアログを起動 |

### 全データレポートセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-all-data-report` | 見出し | ○ | 「全データレポート（申告対象外を含む）」 |
| 当年度総収益 | `display-total-income-all` | 数値表示 | ○ | 金額を右寄せで表示 |
| 当年度総支出 | `display-total-expense-all` | 数値表示 | ○ | 金額を右寄せで表示 |
| 当年度損益 | `display-profit-all` | 数値表示 | ○ | 金額を右寄せで表示 |
| 月別収支テーブル | `table-monthly-all` | テーブル | ○ | 1月〜12月の月別収益・支出・損益を表示 |
| 月リンク | `link-month-detail` | リンク | ○ | 各月をクリックして詳細レポートへ遷移 |
| 収益・支出グラフ | `chart-monthly-income-expense-all` | グラフ | ○ | 月別収益（青棒）・支出（赤棒）の比較グラフ |
| 収益項目内訳見出し | `heading-income-breakdown-all` | 見出し | - | 「収益項目の内訳」 |
| 収益内訳円グラフ | `chart-income-pie-all` | グラフ | - | 収益項目の割合を円グラフで表示 |
| 収益内訳テーブル | `table-income-breakdown-all` | テーブル | - | 収益項目の金額と割合をテーブルで表示 |
| 支出項目内訳見出し | `heading-expense-breakdown-all` | 見出し | - | 「支出項目の内訳」 |
| 支出内訳円グラフ | `chart-expense-pie-all` | グラフ | - | 支出項目の割合を円グラフで表示 |
| 支出内訳テーブル | `table-expense-breakdown-all` | テーブル | - | 支出項目の金額と割合をテーブルで表示 |

### 事業収支レポートセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-business-report` | 見出し | ○ | 「事業収支レポート（申告対象のみ）」 |
| 当年度総収益 | `display-total-income-business` | 数値表示 | ○ | 金額を右寄せで表示 |
| 当年度総支出 | `display-total-expense-business` | 数値表示 | ○ | 金額を右寄せで表示 |
| 当年度損益 | `display-profit-business` | 数値表示 | ○ | 金額を右寄せで表示 |
| 月別収支テーブル | `table-monthly-business` | テーブル | ○ | 1月〜12月の月別収益・支出・損益を表示（申告対象のみ） |
| 月リンク | `link-month-detail-business` | リンク | ○ | 各月をクリックして詳細レポートへ遷移 |
| 収益・支出グラフ | `chart-monthly-income-expense-business` | グラフ | ○ | 月別収益（青棒）・支出（赤棒）の比較グラフ（申告対象のみ） |
| 収益項目内訳見出し | `heading-income-breakdown-business` | 見出し | - | 「収益項目の内訳」 |
| 収益内訳円グラフ | `chart-income-pie-business` | グラフ | - | 収益項目の割合を円グラフで表示（申告対象のみ） |
| 収益内訳テーブル | `table-income-breakdown-business` | テーブル | - | 収益項目の金額と割合をテーブルで表示（申告対象のみ） |
| 支出項目内訳見出し | `heading-expense-breakdown-business` | 見出し | - | 「支出項目の内訳」 |
| 支出内訳円グラフ | `chart-expense-pie-business` | グラフ | - | 支出項目の割合を円グラフで表示（申告対象のみ） |
| 支出内訳テーブル | `table-expense-breakdown-business` | テーブル | - | 支出項目の金額と割合をテーブルで表示（申告対象のみ） |

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

