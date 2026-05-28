# 画面設計書 - 収益登録画面

[← インデックスへ戻る](index.md)

---

## 画面概要

収益データの新規登録・編集を行う画面。区分を選択すると動的に項目セレクトボックスが切り替わる。100,000,000円以上の高額入力時は確認モーダルを表示する。編集時は `editId` パラメータで既存データを初期値として表示する。

- **JSP ファイル**: `Income/income.jsp`
- **URL パス**: `/income`
- **HTTP メソッド**: GET（表示）/ POST（登録・更新）

---

## 画面項目一覧

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|--------|---------------|------|------|------|
| ID（hidden） | `id` | hidden | - | 編集時の収益 ID |
| 戻り先（hidden） | `returnTo` | hidden | - | 編集後の遷移先制御 |
| 区分 | `incomeType` | ラジオボタン | ○ | 事業収入 / 金融・投資収益 / 一時収益 / 非課税・対象外収益 |
| 項目 | `category` | セレクトボックス | ○ | 区分に連動して動的に切替（計39項目） |
| 収益発生日 | `date` | 日付入力 | ○ | YYYY-MM-DD形式 |
| 金額（円） | `amount` | 数値入力 | ○ | 1以上の整数 |
| 受取方法 | `receiveMethodId` | ラジオボタン | ○ | 現金(1) / クレジットカード(2) / 電子決済(3) / 銀行振込(4) / その他(5) |
| 申告対象外 | `businessTartget` | チェックボックス | - | チェック時 `false` を送信（損益計算から除外） |
| 収益詳細 | `details` | テキストエリア | ○ | 1〜255文字、文字数カウント表示 |
| クリアボタン | - | ボタン | - | フォームをリセット |
| 登録/更新ボタン | - | 送信ボタン | - | 新規時「登録」、編集時「更新」 |

### 項目マスタ（区分別）

| 区分 | value | 項目一覧 |
|------|-------|---------|
| 事業収入 | `business` | 商品売上 / サービス売上 / 制作売上 / 開発売上(業務委託) / 開発売上(請負) / コンサル売上 / 広告収益 / アフィリエイト / YouTube収益 / SNS収益 / ライセンス収益 / サブスク収益 / システム利用料 / 手数料収益 / イベント売上 / その他事業収益 |
| 金融・投資収益 | `investment` | 株式配当 / 売却益 / FX収益 / 仮想通貨利益 / ステーキング / 投資信託収益 / 銀行利息 / 外貨利息 / その他金融・投資収益 |
| 一時収益 | `temporary` | 不用品売却 / 臨時収入 / 保険金 / お祝い金 / 懸賞 / 返戻金 / その他一時収益 |
| 非課税・対象外収益 | `nontaxable` | 借入金 / 資本投入費 / 建て替え金 / 預かり金 / 振替 / 補助・給付金 / その他非課税・対象外収益 |

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

