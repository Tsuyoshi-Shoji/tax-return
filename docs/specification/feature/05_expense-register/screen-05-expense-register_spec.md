# 画面設計書 - 支出登録画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

支出データの新規登録・編集を行う画面。区分を選択すると動的に項目セレクトボックスが切り替わる。家事按分チェックを入れると、設定画面の按分率を使って経費対象金額をプレビュー表示する。100,000,000円以上の入力時は確認モーダルを表示する。

- **JSP ファイル**: `Expense/expense.jsp`
- **URL パス**: `/expense`
- **HTTP メソッド**: GET（表示）/ POST（登録・更新）

---

## 画面項目一覧

### ページタイトルセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ページタイトル | `page-title` | 表示 | ○ | 「支出登録」の固定表示 |
| 説明文 | `page-description` | 表示 | ○ | 「支出情報を入力して登録します。」の固定表示 |

### 支出フォームセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 区分ラベル | `label-expense-type` | 表示 | ○ | 「区分」の固定表示 |
| 区分ラジオボタン | `expense-type` | ラジオボタン | ○ | 経費 / 仕入 / その他 |
| 支出項目ラベル | `label-category` | 表示 | ○ | 「支出項目」の固定表示 |
| 支出項目セレクト | `category` | セレクトボックス | ○ | プレースホルダー: `項目を選択してください` |
| 支出発生日ラベル | `label-date` | 表示 | ○ | 「支出発生日」の固定表示 |
| 支出発生日入力 | `date` | 日付入力 | ○ | プレースホルダー: `年/月/日` |
| 金額ラベル | `label-amount` | 表示 | ○ | 「金額」の固定表示 |
| 金額入力 | `amount` | 数値入力 | ○ | プレースホルダー: `例）100000`、単位: `円` |
| 支払方法ラベル | `label-payment-method` | 表示 | ○ | 「支払方法」の固定表示 |
| 支払方法ラジオ | `payment-method` | ラジオボタン | ○ | 現金 / クレジットカード / 電子決済 / 銀行振込 / その他 |
| 申告対象外チェック | `checkbox-exclude-from-return` | チェックボックス | - | 「この支出を確定申告対象外にする」 |

### 家事按分セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 家事按分セクション見出し | `heading-home-apportionment` | 見出し | ○ | 「家事按分」の固定表示 |
| 家事按分適用チェック | `checkbox-apply-apportionment` | チェックボックス | - | 「家事按分を適用する」 |
| 按分率ラベル | `label-apportionment-rate` | 表示 | - | 「家事按分率」の固定表示 |
| 按分率表示 | `display-apportionment-rate` | 表示 | - | 設定中の按分率（`%`） |
| 按分説明文 | `text-apportionment-description` | 表示 | - | 按分方法の説明文 |
| 経費対象金額ラベル | `label-deductible-amount` | 表示 | - | 「経費対象金額」の固定表示 |
| 経費対象金額表示 | `display-deductible-amount` | 表示 | - | リアルタイム計算による経費対象金額 |

### 支出詳細セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 支出詳細ラベル | `label-details` | 表示 | ○ | 「支出詳細」の固定表示 |
| 支出詳細入力 | `details` | テキストエリア | ○ | プレースホルダー: `内容を入力してください` |

### フォーム操作ボタン

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| クリアボタン | `button-clear` | ボタン | ○ | フォーム内容をリセット |
| 登録ボタン | `button-register` | 送信ボタン | ○ | データをサーバーへ送信して登録 |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| 区分変更 | `expenseType` ラジオ change | 項目セレクトボックスを区分に合わせて動的に再描画。区分・カテゴリエラーをクリア |
| 項目変更 | `category` change | カテゴリエラーをクリア |
| 発生日変更 | `date` change | 日付エラーをクリア |
| 金額入力 | `amount` input | 按分プレビューを更新、金額エラーをクリア |
| 支払方法変更 | `paymentMethodId` change | 支払方法エラーをクリア |
| 家事按分チェック変更 | `homeApportionment` change | 按分プレビューパネルの表示切替・経費対象金額を再計算。按分エラーをクリア |
| 詳細入力 | `details` input | 文字数カウント更新、詳細エラーをクリア |
| クリアボタンクリック | click | フォームリセット、項目を初期化、エラークリア、文字数・按分プレビューをリセット |
| フォーム送信 | submit | 按分プレビュー更新 → バリデーション実行 → エラー時はモーダル表示 → 高額確認 → 送信 |
| 高額確認モーダル「確認して送信」クリック | click | 按分プレビュー更新後フォームを送信 |
| モーダル「閉じる」クリック | click | モーダルを閉じる |
| モーダル背景クリック | click | モーダルを閉じる |

---

## バリデーションチェック（フロント）仕様

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| 区分 | 未選択 | 区分を選択してください。 |
| 項目 | 未選択 | 項目を選択してください。 |
| 項目 | 選択値がマスタに存在しない | 選択された項目がマスタに存在しません。 |
| 支出発生日 | 未入力 | 支出発生日を選択してください。 |
| 支出発生日 | 不正な日付形式・存在しない日付 | 存在する日付を選択してください。 |
| 支出発生日 | 未来日付 | 登録日より未来の日付は選択できません。 |
| 金額 | 未入力 | 金額を入力してください。 |
| 金額 | 数値以外 | 金額は数値のみで入力してください。 |
| 金額 | 1未満 | 金額は1円以上で入力してください。 |
| 家事按分率 | 設定値が 0〜100 の範囲外または非数値 | 設定されている家事按分率が不正です。設定画面で0〜100%の範囲に修正してください。 |
| 支払方法 | 未選択 | 支払方法を選択してください。 |
| 支出詳細 | 未入力 | 支出詳細を入力してください。 |
| 支出詳細 | 文字数が 1〜255 の範囲外 | 支出詳細は1〜255文字で入力してください。 |
| 支出詳細 | `< > " ' ` \ ` を含む | 支出詳細に使用できない文字が含まれています。 |
| 金額（警告） | 100,000,000円以上 | 高額支出の確認モーダルを表示（キャンセル可能） |

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller（推定） | `ExpenseController` | `org.example.controller` または `org.example.features.expense.controller` |
| Form | `ExpenseForm` | `org.example.features.expense.form` |
| DTO（編集表示） | `ExpenseEditView` | `org.example.features.expense.dto` |
| DTO（登録結果） | `ExpenseRegistrationResult` | `org.example.features.expense.dto` |
| Service | `ExpenseService` | `org.example.features.expense.service` |
| Service 実装 | `ExpenseServiceImpl` | `org.example.features.expense.service.impl` |
| Entity | `Expense` | `org.example.entity` |
| Entity | `ExpenseCategory` | `org.example.entity` |
| Entity | `ExpenseSubcategory` | `org.example.entity` |
| Entity | `PaymentMethod` | `org.example.entity` |
| Entity | `UserSetting` | `org.example.entity` |
| Repository | `ExpenseRepository` | `org.example.repository` |
| Repository | `ExpenseCategoryRepository` | `org.example.repository` |
| Repository | `ExpenseSubcategoryRepository` | `org.example.repository` |
| Repository | `PaymentMethodRepository` | `org.example.repository` |
| Repository | `UserSettingRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*

