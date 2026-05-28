# 画面設計書 - 支出登録画面

[← インデックスへ戻る](index.md)

---

## 画面概要

支出データの新規登録・編集を行う画面。区分を選択すると動的に項目セレクトボックスが切り替わる。家事按分チェックを入れると、設定画面の按分率を使って経費対象金額をプレビュー表示する。100,000,000円以上の入力時は確認モーダルを表示する。

- **JSP ファイル**: `Expense/expense.jsp`
- **URL パス**: `/expense`
- **HTTP メソッド**: GET（表示）/ POST（登録・更新）

---

## 画面項目一覧

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|--------|---------------|------|------|------|
| ID（hidden） | `id` | hidden | - | 編集時の支出 ID |
| 戻り先（hidden） | `returnTo` | hidden | - | 編集後の遷移先制御 |
| デフォルト按分率（hidden） | `defaultBusinessUseRatio` | hidden | - | 設定画面の按分率（モデルから取得） |
| 経費対象金額（hidden） | `deductibleAmount` | hidden | - | 按分計算後の経費対象金額（JS で設定） |
| 区分 | `expenseType` | ラジオボタン | ○ | 経費 / 公的負担 / 私的利用 |
| 項目 | `category` | セレクトボックス | ○ | 区分に連動して動的に切替（経費17 / 公的負担13 / 私的利用7） |
| 支出発生日 | `date` | 日付入力 | ○ | YYYY-MM-DD形式 |
| 金額（円） | `amount` | 数値入力 | ○ | 1以上の整数 |
| 支払方法 | `paymentMethodId` | ラジオボタン | ○ | 現金(1) / クレジットカード(2) / 電子決済(3) / 銀行振込(4) / その他(5) |
| 家事按分を適用する | `homeApportionment` | チェックボックス | - | チェック時に按分プレビューパネルを表示 |
| 按分プレビュー：按分率表示 | - | 表示 | - | 設定中の按分率（`%`） |
| 按分プレビュー：支出テーブル保存金額 | - | 表示 | - | 入力金額をリアルタイム表示 |
| 按分プレビュー：経費対象金額 | - | 表示 | - | 入力金額 × 按分率を切り捨て計算しリアルタイム表示 |
| 支出詳細 | `details` | テキストエリア | ○ | 1〜255文字、文字数カウント表示 |
| クリアボタン | - | ボタン | - | フォームをリセット |
| 登録/更新ボタン | - | 送信ボタン | - | 新規時「登録」、編集時「更新」 |

### 項目マスタ（区分別）

| 区分 | value | 項目一覧 |
|------|-------|---------|
| 経費 | `expense` | 通信・IT利用料 / 設備・機材 / 外注費 / 広告宣伝費 / 旅費交通費 / 会議費 / 接待交際費 / 地代家賃 / 水道光熱費 / スペース利用料 / 学習・教材費 / 手数料 / 保険（事業保険・賠償責任保険） / 税務相談費 / 法務相談費 / 消耗品費 / 雑費 |
| 公的負担 | `public` | 所得税 / 住民税 / 個人事業税 / 固定資産税 / 自動車税 / 消費税 / 国民健康保険 / 国民年金 / 介護保険 / 証明書発行手数料 / 行政手続料 / 許可申請費 / その他公的支出（罰金・延滞税、各種協会費など） |
| 私的利用 | `private` | 生活費 / 趣味・娯楽 / 教育費 / 医療費 / 交通費 / 交際費 / その他私費(雑費・貯金・投資など) |

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

