# 画面設計書 - ユーザー新規登録画面

[← インデックスへ戻る](index.md)

---

## 画面概要
ユーザーの新規登録を行う画面。メールアドレスとパスワードの入力フォームを表示し、登録処理を行う。登録成功時はログイン画面へ遷移、失敗時はエラーメッセージを表示する。
既登録ユーザーへの導線としてログイン画面へのボタンも提供する。

- **JSP ファイル**: `Auth/user-register.jsp`
- **URL パス**: `/register`
- **HTTP メソッド**: GET（表示）/ POST（新規登録送信）

---

## 画面項目一覧

### 新規登録フォームセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 説明ラベル | `label-description` | 表示 | ○ | メールアドレスとパスワード要件の説明を表示 |
| メールアドレスラベル | `label-address` | 表示 | ○ | 「メールアドレス」の固定表示 |
| メールアドレス入力欄 | `form-address` | メール入力 | ○ | 登録用メールアドレスを入力 |
| パスワードラベル | `label-password` | 表示 | ○ | 「パスワード」の固定表示 |
| パスワード入力欄 | `form-password` | パスワード入力 | ○ | 登録用パスワードを入力 |
| パスワード確認入力欄 | `form-confirm-pass` | パスワード入力 | ○ | 確認用パスワードを入力 |
| 新規登録ボタン | `button-register` | 送信ボタン | ○ | 入力値をサーバーへ送信してユーザー登録 |
| ログインへボタン | `button-to-login` | 遷移ボタン | ○ | ログイン画面（`/login`）へ遷移 |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|---------|---------|---------|
| 新規登録ボタン押下 | submit | フロントバリデーション実行後、`POST /register` を送信 |
| ログインへボタン押下 | click | ログイン画面（`GET /login`）へ遷移 |
| Enter キー送信 | keydown（Enter） | 新規登録フォームを submit（新規登録ボタン押下と同等） |

---

## バリデーションチェック（フロント）仕様

### 新規登録フォーム

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| メールアドレス | 未入力 | メールアドレスを入力してください。 |
| メールアドレス | 正規表現 `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` | メールアドレスは正しい形式で入力してください。 |
| パスワード | 未入力 | パスワードを入力してください。 |
| パスワード | 8〜72文字の範囲外 | パスワードは8〜72文字で入力してください。 |
| パスワード（確認） | 未入力 | 確認用パスワードを入力してください。 |
| パスワード（確認） | パスワード入力欄との不一致 | パスワードが異なります。確認してください。 |

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Entity（実装済） | `User` | `org.example.entity` |
| Repository（実装済） | `UserRepository` | `org.example.repository` |
| Entity（実装済） | `UserSetting` | `org.example.entity` |
| Repository（実装済） | `UserSettingRepository` | `org.example.repository` |
| Controller（未実装・想定） | `AuthController` | `org.example.features.auth.controller` |
| Service（未実装・想定） | `AuthService` | `org.example.features.auth.service` |
| Service 実装（未実装・想定） | `AuthServiceImpl` | `org.example.features.auth.service.impl` |
| Form（未実装・想定） | `UserRegisterForm` | `org.example.features.auth.form` |
| DTO（未実装・想定） | `UserRegisterResult` | `org.example.features.auth.dto` |

---

*作成日: 2026-05-26*
