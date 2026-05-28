# 画面設計書 - 設定画面

[← インデックスへ戻る](index.md)

---

## 画面概要

ユーザーのプロフィール（ユーザー名・メールアドレス）の編集、パスワード変更、家事按分率の設定、アカウント無効化・削除を行う画面。4つの独立したフォームで構成され、それぞれ個別に POST 送信する。

- **JSP ファイル**: `Setting/settings.jsp`
- **URL パス**: `/settings`
- **HTTP メソッド**: GET（表示）/ POST（各フォーム送信）

---

## 画面項目一覧

### プロフィールセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|--------|---------------|------|------|------|
| アカウントステータス | - | 表示 | - | `setting.accountStatus` |
| ユーザー名 | `username` | テキスト | ○ | 1〜100文字。`setting.username` を初期値として表示 |
| 登録メールアドレス | `email` | メール | ○ | `setting.email` を初期値として表示 |
| プロフィールを保存 | - | 送信ボタン | - | `POST /settings/profile` |

### パスワード変更セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|--------|---------------|------|------|------|
| 現在のパスワード | `currentPassword` | パスワード | ○ | - |
| 新しいパスワード | `newPassword` | パスワード | ○ | 8〜72文字 |
| 新しいパスワード（確認） | `confirmPassword` | パスワード | ○ | `newPassword` と一致必須 |
| パスワードを変更 | - | 送信ボタン | - | `POST /settings/password` |

### 家事按分の設定セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|--------|---------------|------|------|------|
| 家事按分率 | `defaultBusinessUseRatio` | 数値 | ○ | 0〜100（小数点2桁まで）。`setting.defaultBusinessUseRatio` を初期値として表示 |
| 経費対象金額プレビュー | - | 表示 | - | 10,000円に対する経費対象金額をリアルタイム計算表示 |
| 家事按分率を保存 | - | 送信ボタン | - | `POST /settings/business-use-ratio` |

### アカウント操作セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|--------|---------------|------|------|------|
| 無効化確認テキスト | `confirmationText` | テキスト | - | 「無効化」と入力することで送信を許可 |
| ユーザーを無効化 | - | 送信ボタン | - | `POST /settings/disable` |
| 削除確認テキスト | `confirmationText` | テキスト | - | 「削除」と入力することで送信を許可 |
| ユーザーを削除 | - | 送信ボタン | - | `POST /settings/delete` |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| プロフィールフォーム送信 | submit | バリデーション実行 → 通過時 `POST /settings/profile` |
| パスワード変更フォーム送信 | submit | バリデーション実行 → 通過時 `POST /settings/password` |
| 家事按分率入力 | `defaultBusinessUseRatio` input | 経費対象金額プレビューをリアルタイム更新（10,000円 × 按分率 / 100、切り捨て）。按分率エラーをクリア |
| 家事按分フォーム送信 | submit | バリデーション実行 → 通過時 `POST /settings/business-use-ratio` |
| 無効化フォーム送信 | submit | 確認テキストバリデーション → `window.confirm` ダイアログ → OK 時 `POST /settings/disable` |
| 削除フォーム送信 | submit | 確認テキストバリデーション → `window.confirm` ダイアログ → OK 時 `POST /settings/delete` |

---

## バリデーションチェック（フロント）仕様

### プロフィールフォーム

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| ユーザー名 | 1〜100文字 | ユーザー名は1〜100文字で入力してください。 |
| メールアドレス | 正規表現 `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` | メールアドレスは正しい形式で入力してください。 |

### パスワード変更フォーム

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| 現在のパスワード | 未入力 | 現在のパスワードを入力してください。 |
| 新しいパスワード | 8〜72文字の範囲外 | 新しいパスワードは8〜72文字で入力してください。 |
| 確認用パスワード | 新しいパスワードと不一致 | 新しいパスワードと確認用パスワードが一致しません。 |

### 家事按分フォーム

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| 家事按分率 | 0〜100 の範囲外または非数値 | 家事按分率は0〜100%の範囲で入力してください。 |

### アカウント操作フォーム

| 項目 | チェック内容 | エラーメッセージ / 挙動 |
|------|------------|----------------------|
| 無効化確認テキスト | 「無効化」以外 | 確認欄に「無効化」と入力してください。 |
| 削除確認テキスト | 「削除」以外 | 確認欄に「削除」と入力してください。 |
| 操作確認（共通） | `window.confirm` ダイアログ | キャンセル時はフォーム送信を中断 |

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller | `SettingController` | `org.example.features.Setting.controller` |
| Form（プロフィール） | `ProfileForm` | `org.example.features.Setting.form` |
| Form（パスワード） | `PasswordForm` | `org.example.features.Setting.form` |
| Form（家事按分率） | `BusinessUseRatioForm` | `org.example.features.Setting.form` |
| Form（アカウント操作） | `AccountActionForm` | `org.example.features.Setting.form` |
| DTO（設定表示） | `SettingView` | `org.example.features.Setting.dto` |
| DTO（操作結果） | `SettingOperationResult` | `org.example.features.Setting.dto` |
| Service | `SettingService` | `org.example.features.Setting.service` |
| Service 実装 | `SettingServiceImpl` | `org.example.features.Setting.service.impl` |
| Entity | `User` | `org.example.entity` |
| Entity | `UserSetting` | `org.example.entity` |
| Repository | `UserRepository` | `org.example.repository` |
| Repository | `UserSettingRepository` | `org.example.repository` |

---

*作成日: 2026-05-26*

