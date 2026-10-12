# 画面設計書 - 損益検索・一覧画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

収益・支出データを条件指定で検索し、収益一覧・支出一覧をページネーション付きで表示する。損益サマリー（総収益・総支出・損益金額）も合わせて表示する。各行の「詳細」リンクで損益詳細画面へ遷移する。

- **JSP ファイル**: `ProfitLoss/profit-loss.jsp`
- **URL パス**: `/profit-loss`
- **HTTP メソッド**: GET（検索・表示）

---

## 画面項目一覧

### ページタイトルセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ページタイトル | `page-title` | 表示 | ○ | 「損益一覧」の固定表示 |
| 説明文 | `page-description` | 表示 | ○ | 「収益・支出の一覧を確認できます。期間や条件で絞り込んで検索することができます。」の固定表示 |

### 検索条件セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 発生日Fromラベル | `label-date-from` | 表示 | - | 「発生日From」の固定表示 |
| 発生日From入力 | `date-from` | カレンダー入力 | - | 検索開始日を指定 |
| 発生日Toラベル | `label-date-to` | 表示 | - | 「発生日To」の固定表示 |
| 発生日To入力 | `date-to` | カレンダー入力 | - | 検索終了日を指定 |
| 区分ラベル | `label-types` | 表示 | - | 「区分」の固定表示 |
| 区分チェックボックス | `types` | チェックボックス | - | 収益 / 支出（複数選択可） |
| 申告対象データラベル | `label-tax-target` | 表示 | - | 「申告対象データ」の固定表示 |
| 申告対象データラジオ | `tax-target` | ラジオボタン | - | 表示する / 表示しない |
| 項目ラベル | `label-items` | 表示 | - | 「項目」の固定表示 |
| 項目セレクト | `item` | セレクトボックス | - | 項目を選択（区分に連動、`income:{id}` / `expense:{id}`） |
| 金額Fromラベル | `label-amount-from` | 表示 | - | 「金額From」の固定表示 |
| 金額From入力 | `amount-from` | 数値入力 | - | プレースホルダー: `例）10000` |
| 金額Toラベル | `label-amount-to` | 表示 | - | 「金額To」の固定表示 |
| 金額To入力 | `amount-to` | 数値入力 | - | プレースホルダー: `例）100000` |
| 条件クリアボタン | `button-clear-conditions` | ボタン | - | 検索条件をリセット |
| 検索ボタン | `button-search` | 送信ボタン | ○ | 条件で検索を実行 |

### 損益集計セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 損益集計見出し | `heading-profit-loss-summary` | 見出し | ○ | 「損益集計」の固定表示 |
| 損益集計テーブル | `table-profit-loss-summary` | テーブル | ○ | 総収益・総支出・損益金額を表示 |

### 収益一覧セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 収益一覧見出し | `heading-income-list` | 見出し | ○ | 「収益一覧」の固定表示 |
| 件数・ページネーション | `pagination-income` | 表示 | ○ | 「全[件数]件 / ページ10件表示」とページネーション |
| 収益テーブル | `table-income-list` | テーブル | ○ | 発生日、区分、項目、詳細、金額、申告対象、詳細ボタンを表示 |

### 支出一覧セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 支出一覧見出し | `heading-expense-list` | 見出し | ○ | 「支出一覧」の固定表示 |
| 件数・ページネーション | `pagination-expense` | 表示 | ○ | 「全[件数]件 / ページ10件表示」とページネーション |
| 支出テーブル | `table-expense-list` | テーブル | ○ | 発生日、区分、項目、詳細、金額、申告対象、詳細ボタンを表示 |
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
| クリアボタンクリック | click | `/profit-loss` に遷移し検索条件とページを初期化 |
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
| Controller | `ProfitLossController` | `org.example.controller` |
| Form | `ProfitLossSearchForm` | `org.example.features.profitloss.form` |
| DTO（一覧行） | `ProfitLossListRow` | `org.example.features.profitloss.dto` |
| DTO（画面データ） | `ProfitLossPageData` | `org.example.features.profitloss.dto` |
| Service | `ProfitLossService` | `org.example.features.profitloss.service` |
| Service 実装 | `ProfitLossServiceImpl` | `org.example.features.profitloss.service.impl` |
| Entity | `IncomeEntry` | `org.example.entity` |
| Entity | `ExpenseEntry` | `org.example.entity` |
| Repository | `IncomeEntryRepository` | `org.example.repository` |
| Repository | `ExpenseEntryRepository` | `org.example.repository` |

---

## 実装補足（2026-10）

- 区分チェックは `income` / `expense` の複数選択。未選択で送信された場合は両方を対象として扱う。
- 申告対象外データは `taxTarget=show|hide` で切替。`hide` の場合は `excludeFromReturn=true` データを除外。
- バリデーション（フロント・サーバー）:
  - 発生日: 実在日付
  - 発生日To: 発生日From以降
  - 金額: 数値（0以上）
  - 金額To: 金額From以上
- ページングは収益/支出それぞれ独立し、`incomePage` / `expensePage`（10件/ページ）で管理。
- 画面内「詳細」リンクは `/profit-loss/detail?recordType={income|expense}&id={id}`。

---

*作成日: 2026-05-26*

