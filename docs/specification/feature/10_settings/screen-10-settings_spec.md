# 画面設計書 - 設定画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

ユーザーのプロフィール（ユーザー名・メールアドレス）の編集、パスワード変更、家事按分率の設定、アカウント無効化・削除を行う画面。4つの独立したフォームで構成され、それぞれ個別に POST 送信する。

- **JSP ファイル**: `Setting/settings.jsp`
- **URL パス**: `/settings`
- **HTTP メソッド**: GET（表示）/ POST（各フォーム送信）

---

## 画面項目一覧

### ページタイトルセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| ページタイトル | `page-title` | 表示 | ○ | 「各種設定」の固定表示 |
| 説明文 | `page-description` | 表示 | ○ | 「アカウント情報や各種設定の確認・変更を行います。」の固定表示 |

### プロフィール設定セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-profile` | 見出し | ○ | 「プロフィール」 |
| ユーザー名ラベル | `label-username` | 表示 | ○ | 「ユーザー名」の固定表示 |
| ユーザー名表示 | `display-username` | 表示 | ○ | ログインユーザー名を表示（山田太郎） |
| メールアドレスラベル | `label-email` | 表示 | ○ | 「登録メールアドレス」の固定表示 |
| メールアドレス表示 | `display-email` | 表示 | ○ | 登録メールアドレスを表示（taro.yamada@example.com） |
| アカウント状態ラベル | `label-status` | 表示 | ○ | 「アカウントステータス」の固定表示 |
| アカウント状態表示 | `display-status` | 表示 | ○ | アカウント状態を表示（Active） |
| プロフィール編集ボタン | `button-edit-profile` | ボタン | ○ | プロフィール編集画面へ遷移 |

### パスワード変更セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-password` | 見出し | ○ | 「パスワードの変更」 |
| 現在のパスワードラベル | `label-current-password` | 表示 | ○ | 「現在のパスワード」の固定表示 |
| 現在のパスワード入力 | `input-current-password` | パスワード入力 | ○ | マスク表示、右側に表示切替ボタン |
| パスワード表示切替ボタン | `toggle-current-password` | ボタン | - | パスワードの表示/マスク切替 |
| 新しいパスワードラベル | `label-new-password` | 表示 | ○ | 「新しいパスワード」の固定表示 |
| 新しいパスワード入力 | `input-new-password` | パスワード入力 | ○ | マスク表示、右側に表示切替ボタン |
| パスワード表示切替ボタン | `toggle-new-password` | ボタン | - | パスワードの表示/マスク切替 |
| 新しいパスワード再入力ラベル | `label-confirm-password` | 表示 | ○ | 「新しいパスワード（再入力）」の固定表示 |
| 新しいパスワード再入力 | `input-confirm-password` | パスワード入力 | ○ | マスク表示、右側に表示切替ボタン |
| パスワード表示切替ボタン | `toggle-confirm-password` | ボタン | - | パスワードの表示/マスク切替 |
| パスワード変更ボタン | `button-update-password` | 送信ボタン | ○ | パスワードを変更（POST /settings/password） |

### 家事按分設定セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-home-apportionment` | 見出し | ○ | 「家事按分の設定」 |
| 説明文 | `text-apportionment-description` | 表示 | ○ | 「設定した家事按分率は、支出登録画面の計算に反映されます。」の固定表示 |
| 家事按分率ラベル | `label-apportionment-rate` | 表示 | ○ | 「家事按分率」の固定表示 |
| 家事按分率入力 | `input-apportionment-rate` | 数値入力 | ○ | 0～100の整数、プレースホルダー: `例）30`、単位: `%` |
| 設定例説明 | `text-apportionment-example` | 表示 | ○ | 「※ 支出登録時の反映例」の固定表示 |
| 入力金額例 | `display-example-amount` | 表示 | - | 「支出金額: 100,000円」 |
| 按分率計算例 | `display-example-calculation` | 表示 | - | 「経費対象金額: [計算結果]円」 |
| 家事按分率保存ボタン | `button-save-apportionment` | 送信ボタン | ○ | 家事按分率を保存（POST /settings/business-use-ratio） |

### アカウント操作セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| セクション見出し | `heading-account-operations` | 見出し | ○ | 「アカウント操作」 |
| 無効化説明文 | `text-disable-description` | 表示 | ○ | 「このアカウントを無効化します。ログインできなくなります。」の固定表示 |
| 無効化確認入力ラベル | `label-disable-confirmation` | 表示 | ○ | 「『無効化』と入力してください」の固定表示 |
| 無効化確認入力 | `input-disable-confirmation` | テキスト入力 | ○ | 確認用テキスト入力 |
| アカウント無効化ボタン | `button-disable-account` | 送信ボタン | ○ | アカウントを無効化（POST /settings/disable） |
| 削除説明文 | `text-delete-description` | 表示 | ○ | 「このアカウントと全てのデータを完全に削除します。この操作は取り消せません。」の固定表示 |
| 削除確認入力ラベル | `label-delete-confirmation` | 表示 | ○ | 「『削除』と入力してください」の固定表示 |
| 削除確認入力 | `input-delete-confirmation` | テキスト入力 | ○ | 確認用テキスト入力 |
| アカウント削除ボタン | `button-delete-account` | 送信ボタン | ○ | アカウントを削除（POST /settings/delete） |

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

