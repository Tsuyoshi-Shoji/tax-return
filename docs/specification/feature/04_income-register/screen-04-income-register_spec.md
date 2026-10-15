# 画面設計書 - 収益登録画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

収益データの新規登録・編集を行う画面。区分を選択すると動的に項目セレクトボックスが切り替わる。100,000,000円以上の高額入力時は確認モーダルを表示する。編集時は `editId` パラメータで既存データを初期値として表示する。

- **JSP ファイル**: `Income/income.jsp`
- **URL パス**: `/income`
- **HTTP メソッド**: GET（表示）/ POST（登録・更新）

---

## 画面項目一覧

### ページタイトルセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ページタイトル | `page-title` | 表示 | ○ | 「収益登録」の固定表示 |
| 説明文 | `page-description` | 表示 | ○ | 「収益情報を入力して登録します。」の固定表示 |

### 収益フォームセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 区分ラベル | `label-income-type` | 表示 | ○ | 「区分」の固定表示 |
| 区分ラジオボタン | `income-type` | ラジオボタン | ○ | 売上 / 雑収入 / その他 |
| 収益項目ラベル | `label-category` | 表示 | ○ | 「収益項目」の固定表示 |
| 収益項目セレクト | `category` | セレクトボックス | ○ | プレースホルダー: `項目を選択してください` |
| 収益発生日ラベル | `label-date` | 表示 | ○ | 「収益発生日」の固定表示 |
| 収益発生日入力 | `date` | 日付入力 | ○ | プレースホルダー: `年/月/日` |
| 金額ラベル | `label-amount` | 表示 | ○ | 「金額」の固定表示 |
| 金額入力 | `amount` | 数値入力 | ○ | プレースホルダー: `例）100000`、単位: `円` |
| 受取方法ラベル | `label-receive-method` | 表示 | ○ | 「受取方法」の固定表示 |
| 受取方法ラジオ | `receive-method` | ラジオボタン | ○ | 現金 / クレジットカード / 電子決済 / 銀行振込 / その他 |
| 申告対象外チェック | `checkbox-exclude-from-return` | チェックボックス | - | 「この収益を確定申告対象外にする」 |
| 収益詳細ラベル | `label-details` | 表示 | ○ | 「収益詳細」の固定表示 |
| 収益詳細入力 | `details` | テキストエリア | ○ | プレースホルダー: `内容を入力してください` |

### 収益項目の選択肢

| 区分 | 選択肢 |
|------|--------|
| 売上 | SES・準委任契約報酬 / 受託システム開発 / IT講師・技術研修 / 技術記事・コンテンツ制作 / 自社サービス・SaaS利用料 / ソフトウェア・ライセンス販売 / 広告・アフィリエイト収益 / その他売上 |
| 雑収入 | 補助金・助成金（事業関連） / 事業関連の保険金・補償金 / 取引先からの謝礼金 / 事業関連のキャッシュバック / 事業関連の雑収益 / その他雑収入 |
| その他 | 事業主借入金（事業主借） / 借入金入金 / 預り金の受入 / 立替金の回収 / 誤入金・返金調整 / その他の資金移動 |

### フォーム操作ボタン

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| クリアボタン | `button-clear` | ボタン | ○ | フォーム内容をリセット |
| 登録ボタン | `button-register` | 送信ボタン | ○ | データをサーバーへ送信して登録 |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| 区分変更 | `incomeType` ラジオ change | 項目セレクトボックスを区分に合わせて動的に再描画。区分・カテゴリエラーをクリア |
| 項目変更 | `category` change | カテゴリエラーをクリア |
| 発生日変更 | `date` change | 日付エラーをクリア |
| 金額入力 | `amount` input | 金額エラーをクリア |
| 受取方法変更 | `receiveMethodId` change | 受取方法エラーをクリア |
| 詳細入力 | `details` input | 文字数カウント更新、詳細エラーをクリア |
| クリアボタンクリック | click | フォームリセット、項目を初期化、エラークリア、文字数カウントを 0 に |
| フォーム送信 | submit | バリデーション実行 → エラー時は入力エラーモーダル表示 → 高額確認 → 送信 |
| 高額確認モーダル「確認して送信」クリック | click | モーダルを閉じてフォームを送信 |
| モーダル「閉じる」クリック | click | モーダルを閉じる |
| モーダル背景クリック | click | モーダルを閉じる |

---

## バリデーションチェック（フロント）仕様

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| 区分 | 未選択 | 区分を選択してください。 |
| 項目 | 未選択 | 項目を選択してください。 |
| 項目 | 選択値がマスタに存在しない | 選択された項目がマスタに存在しません。 |
| 収益発生日 | 未入力 | 収益発生日を選択してください。 |
| 収益発生日 | 不正な日付形式・存在しない日付 | 存在する日付を選択してください。 |
| 収益発生日 | 未来日付 | 登録日より未来の日付は選択できません。 |
| 金額 | 未入力 | 金額を入力してください。 |
| 金額 | 数値以外 | 金額は数値のみで入力してください。 |
| 金額 | 1未満 | 金額は1円以上で入力してください。 |
| 受取方法 | 未選択 | 受取方法を選択してください。 |
| 収益詳細 | 未入力 | 収益詳細を入力してください。 |
| 収益詳細 | 文字数が 1〜255 の範囲外 | 収益詳細は1〜255文字で入力してください。 |
| 収益詳細 | `< > " ' ` \ ` を含む | 収益詳細に使用できない文字が含まれています。 |
| 金額（警告） | 100,000,000円以上 | 高額収益の確認モーダルを表示（キャンセル可能） |

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller（推定） | `IncomeController` | `org.example.controller` または `org.example.features.Income.controller` |
| Form | `IncomeForm` | `org.example.features.Income.form` |
| DTO（編集表示） | `IncomeEditView` | `org.example.features.Income.dto` |
| DTO（登録結果） | `IncomeRegistrationResult` | `org.example.features.Income.dto` |
| Service | `IncomeService` | `org.example.features.Income.service` |
| Service 実装 | `IncomeServiceImpl` | `org.example.features.Income.service.impl` |
| Entity | `Income` | `org.example.entity` |
| Entity | `IncomeCategory` | `org.example.entity` |
| Entity | `IncomeSubcategory` | `org.example.entity` |
| Entity | `ReceiveMethod` | `org.example.entity` |
| Repository | `IncomeRepository` | `org.example.repository` |
| Repository | `IncomeCategoryRepository` | `org.example.repository` |
| Repository | `IncomeSubcategoryRepository` | `org.example.repository` |
| Repository | `ReceiveMethodRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*
