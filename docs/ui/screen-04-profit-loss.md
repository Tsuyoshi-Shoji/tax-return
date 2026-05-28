# 画面設計書 - 損益検索・一覧画面

[← インデックスへ戻る](index.md)

---

## 画面概要

収益・支出データを条件指定で検索し、収益一覧・支出一覧をページネーション付きで表示する。損益サマリー（総収益・総支出・損益金額）も合わせて表示する。各行の「詳細」リンクで損益詳細画面へ遷移する。

- **JSP ファイル**: `ProfitLoss/profit-loss.jsp`
- **URL パス**: `/profit-loss`
- **HTTP メソッド**: GET（検索・表示）/ POST（削除は詳細画面から）

---

## 画面項目一覧

### 検索フォーム

| 項目名             | 項目 ID / name      | 種別       | 必須 | 説明                                                   |
|-----------------|-------------------|----------|----|------------------------------------------------------|
| 発生日 From        | `dateFrom`        | 日付入力     | -  | 省略時は全件対象                                             |
| 発生日 To          | `dateTo`          | 日付入力     | -  | 省略時は全件対象                                             |
| 区分（複数選択）        | `types`           | チェックボックス | -  | 経費 / 公的負担 / 私的利用 / 事業収入 / 金融・投資収益 / 一時収益 / 非課税・対象外収益 |
| 申告対象外データ        | `showTaxExcluded` | ラジオボタン   | -  | 表示する / 表示しない                                         |
| 項目（カスタムドロップダウン） | `items`（hidden）   | カスタム選択   | -  | 区分に連動した項目の複数選択。区分未選択時は全区分の項目を表示                      |
| 項目検索            | `itemSearch`      | 検索入力     | -  | 項目ドロップダウン内で項目名を部分一致検索し、候補を絞り込む                       |
| 項目サジェスト候補       | `itemSuggestions` | 候補表示     | -  | 項目検索に応じて部分一致候補を予測候補として表示（最大8件）                         |
| 金額 From         | `amountFrom`      | 数値入力     | -  | 省略時は全件対象                                             |
| 金額 To           | `amountTo`        | 数値入力     | -  | 省略時は全件対象                                             |
| 収益ページ（hidden）   | `incomePage`      | hidden   | -  | 収益一覧の現在ページ番号                                         |
| 支出ページ（hidden）   | `expensePage`     | hidden   | -  | 支出一覧の現在ページ番号                                         |
| クリアボタン          | -                 | ボタン      | -  | 条件をリセットして再検索                                         |
| 検索ボタン           | -                 | 送信ボタン    | -  | GET 送信                                               |

### 損益サマリー

| 項目名 | 種別 | モデル属性 |
|--------|------|-----------|
| 総収益金額 | 表示 | `totalIncome` |
| 総支出金額 | 表示 | `totalExpense` |
| 損益金額 | 表示 | `profitAmount` |

### 収益一覧テーブル

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 発生日 | 表示 | `row.date` |
| 区分 | 表示 | `row.type` |
| 項目 | 表示 | `row.item` |
| 金額 | 表示 | `row.amount`（円表示） |
| 詳細リンク | リンク | `/profit-loss/detail?recordType=income&id=...` |
| ページネーション | ページング | `incomeCurrentPage` / `incomeTotalPages`（10件/ページ） |

### 支出一覧テーブル

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| 発生日 | 表示 | `row.date` |
| 区分 | 表示 | `row.type` |
| 項目 | 表示 | `row.item` |
| 金額 | 表示 | `row.amount`（円表示） |
| 詳細リンク | リンク | `/profit-loss/detail?recordType=expense&id=...` |
| ページネーション | ページング | `expenseCurrentPage` / `expenseTotalPages`（10件/ページ） |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| 区分チェックボックス変更 | change | 項目ドロップダウンの選択肢を区分に合わせて再描画。`items` をクリア |
| 項目ドロップダウン表示ボタンクリック | click | 項目選択ドロップダウンを開閉 |
| 項目検索入力 | input | 項目ドロップダウン内の候補を部分一致で絞り込み、表示件数を更新 |
| 項目サジェスト候補クリック | click | 対応する項目の選択状態をON/OFFし、`items` hiddenと表示ラベルへ反映 |
| 項目ドロップダウン外クリック | click（document） | ドロップダウンを閉じる |
| 項目チェックボックス変更 | change | 選択済みアイテムを `items` hidden input に反映・表示ラベルを更新 |
| クリアボタンクリック | click | フォームリセット・`items` クリア・ページを 1 にリセット・フォーム送信 |
| 検索ボタンクリック（フォーム送信） | submit | バリデーション実行 → エラー時はエラーモーダル表示して中断 → 通過時 GET 送信 |
| ページネーション「前へ」クリック | click | 対応する hidden ページ番号を -1 してフォーム送信 |
| ページネーション「次へ」クリック | click | 対応する hidden ページ番号を +1 してフォーム送信 |
| ページネーションページ番号クリック | click | 対応する hidden ページ番号を指定値にしてフォーム送信 |
| エラーモーダル「閉じる」クリック | click | エラーモーダルを閉じる |
| エラーモーダル背景クリック | click | エラーモーダルを閉じる |

---

## バリデーションチェック（フロント）仕様

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| 発生日 From | 不正な日付形式・存在しない日付 | 発生日Fromには存在する日付を指定してください。 |
| 発生日 To | 不正な日付形式・存在しない日付 | 発生日Toには存在する日付を指定してください。 |
| 発生日 From / To | To が From より前 | 発生日Toは発生日From以降の日付を指定してください。 |
| 金額 From | 数値以外 | 金額Fromは数値で入力してください。 |
| 金額 To | 数値以外 | 金額Toは数値で入力してください。 |
| 金額 From / To | To が From 未満 | 金額Toは金額From以上を指定してください。 |

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller（推定） | `ProfitLossController` | `org.example.controller` または `org.example.features.ProfitLoss.controller` |
| Form | `ProfitLossSearchForm` | `org.example.features.ProfitLoss.form` |
| DTO（一覧行） | `ProfitLossListRow` | `org.example.features.ProfitLoss.dto` |
| DTO（検索結果） | `ProfitLossSearchResult` | `org.example.features.ProfitLoss.dto` |
| Service | `ProfitLossService` | `org.example.features.ProfitLoss.service` |
| Service 実装 | `ProfitLossServiceImpl` | `org.example.features.ProfitLoss.service.impl` |
| Entity | `Income` | `org.example.entity` |
| Entity | `Expense` | `org.example.entity` |
| Repository | `IncomeRepository` | `org.example.repository` |
| Repository | `ExpenseRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*

