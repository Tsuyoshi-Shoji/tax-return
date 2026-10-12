# 画面設計書 - AIアシスタント画面

[← インデックスへ戻る](../index.md)

---

## 画面概要

ChatGPT API との連携を通じて、税務・会計に関する質問や相談をAIアシスタントに行う画面。全画面の右下に固定されたアイコンをクリックすることでチャットウィンドウが表示される。サイドメニューからの遷移導線は設けない。ユーザーはこのアシスタントを通じて、個人事業主向けの税務・会計申告のサポートを受ける。

- **Component ファイル**: `Assistant/AIAssistantWidget.jsx` / `Assistant/ChatWindow.jsx`
- **表示方式**: 常時右下に固定アイコン表示（フローティング）
- **HTTP メソッド**: WebSocket / API POST（チャットメッセージ送信、ChatGPT連携）
- **外部API**: OpenAI ChatGPT API

---

## 画面項目一覧

### フローティングアイコンセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| AIアシスタントアイコン | `ai-assistant-icon` | アイコンボタン | ○ | 常に画面右下に固定表示、AIの吹き出しアイコン（サイズ: 56px × 56px） |
| アイコンホバー状態 | `ai-assistant-icon:hover` | 状態表示 | - | ホバー時に背景色が変化、アイコン下に「AIアシスタント」のテキスト表示 |
| アイコンクリック動作 | `ai-assistant-icon:click` | イベント | ○ | クリック時にチャットウィンドウを表示（初回表示時はアニメーション） |

### チャットウィンドウセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| チャットウィンドウ | `chat-window` | コンテナ | ○ | 画面右下に固定表示（サイズ: 400px × 600px、レスポンシブで調整） |
| ウィンドウヘッダー | `chat-header` | ヘッダー | ○ | 「AIアシスタント」のタイトル + 最小化ボタン（-）+ 閉じるボタン（×） |
| 最小化ボタン | `button-minimize-chat` | ボタン | ○ | クリック時にウィンドウを折りたたむ（アイコンのみ表示） |
| 閉じるボタン | `button-close-chat` | ボタン | ○ | クリック時にチャットウィンドウを非表示（会話履歴は保持） |

### チャットメッセージエリアセクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| メッセージスクロールエリア | `chat-messages-container` | スクロール可能な領域 | ○ | ウィンドウ内のメッセージ表示エリア（自動スクロール） |
| ユーザーメッセージ吹き出し | `user-message-bubble` | メッセージ表示 | ○ | 右側に配置、背景色: 青系（#007BFF など）、テキスト: 白 |
| AIメッセージ吹き出し | `ai-message-bubble` | メッセージ表示 | ○ | 左側に配置、背景色: グレー系（#E9ECEF など）、テキスト: 黒 |
| メッセージのタイムスタンプ | `message-timestamp` | 表示 | - | 各メッセージ右下に「HH:MM」形式で表示 |
| AIメッセージ読み込み中インジケータ | `ai-typing-indicator` | アニメーション表示 | - | AIが応答中の場合、「●●●」のアニメーション表示 |
| メッセージリセット表示 | `text-start-new-conversation` | リセットボタン/リンク | - | 一定の条件で新規会話開始のオプションを表示 |

### ユーザー入力セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| テキスト入力フィールド | `message-input` | テキスト入力 | ○ | プレースホルダー: 「税務・会計のご質問・相談などをお気軽にお聞きください」 |
| 送信ボタン | `button-send-message` | ボタン | ○ | 入力欄右側の「送信」ボタン（紙飛行機アイコン）、キーボード: Enter キーで送信 |
| 送信ボタン非活性状態 | `button-send-message:disabled` | 状態表示 | - | 入力が空の場合は非活性（グレーアウト） |
| 文字数カウンター | `message-char-count` | 表示 | - | 現在の入力文字数 / 最大文字数（例: 45/2000） |

### チャット機能セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| 会話リセットボタン | `button-reset-conversation` | ボタン | - | チャットウィンドウ内のリセットボタン、会話履歴をクリア（確認ダイアログ付き） |
| コンテキスト情報送信 | `send-user-context` | 隠し情報 | - | ユーザーの申告状態・現在の画面情報・登録済みデータをバックグラウンドで送信 |
| APIエラーメッセージ | `error-message-api` | エラー表示 | - | ChatGPT API 連携エラー時に「申し訳ありません。現在AIアシスタントが利用できません。」を表示 |
| ネットワークエラーメッセージ | `error-message-network` | エラー表示 | - | 通信障害時に「ネットワーク接続を確認してください。」を表示 |
| トークン上限警告 | `warning-token-limit` | 警告表示 | - | 会話が長くなった場合、「新規会話を開始することをお勧めします」を表示 |

### 回答の補助機能セクション

| 項目名 | 項目 ID / name | 種別 | 必須 | 説明 |
|------|----------------|------|------|------|
| クイックアクション提案 | `quick-action-buttons` | ボタングループ | - | AIが提案する次のアクション（例: 「支出登録へ」「レポートを見る」） |
| データへのクイックリンク | `quick-link-to-screen` | リンク | - | AI回答内から関連する画面へのクイックリンク（例: 「支出登録画面を開く」） |
| 回答のコピーボタン | `button-copy-message` | ボタン | - | AIの回答をクリップボードにコピー |
| 回答の評価ボタン | `button-rate-message` | ボタングループ | - | 「参考になった」「参考にならなかった」の評価送信 |

---

## イベント一覧

| イベント | トリガー | 処理内容 |
|----------|---------|---------|
| AIアシスタントアイコンクリック | クリック | チャットウィンドウを表示（初回のみアニメーション、2回目以降はトグル） |
| チャットウィンドウ最小化 | 最小化ボタン click | ウィンドウを折りたたむ（アイコンのみ表示） |
| チャットウィンドウ閉じる | 閉じるボタン click | チャットウィンドウを非表示 |
| メッセージ送信 | 送信ボタン click / Enter キー | ユーザーメッセージを API `/api/chat/send` に POST → AIメッセージ取得 → メッセージ一覧に追加 |
| テキスト入力 | `message-input` input | 入力文字数に応じて送信ボタンの活性/非活性を切り替え、文字数カウンター更新 |
| 会話リセット | `button-reset-conversation` click | 確認ダイアログ表示 → OK 時 `POST /api/chat/reset` → メッセージ一覧をクリア |
| AI応答取得 | WebSocket / API 応答受信 | ストリーミングまたは一括でAI応答を受信 → 吹き出しにテキスト挿入 → メッセージ追加アニメーション |
| コピーボタンクリック | `button-copy-message` click | AIメッセージのテキストをクリップボードにコピー → 「コピーしました」トースト表示 |
| 回答評価送信 | 「参考になった」「参考にならなかった」 click | 評価を `POST /api/chat/rate-message` に送信 → 解析用データとして保存 |
| クイックアクション実行 | `quick-action-buttons` click | クリックされたアクション（例: `/income` へ遷移） |
| ウィンドウ外クリック | ウィンドウ外をクリック | チャットウィンドウを非表示（設定で toggle/close を選択可能） |
| ネットワークエラー | API 通信失敗 | エラーメッセージ表示 → リトライボタン表示 |
| ChatGPT API エラー | API 回答エラー | エラーメッセージ表示 → ユーザーへの案内文表示 |

---

## バリデーション・チェック（フロント）仕様

### メッセージ入力

| 項目 | チェック内容 | エラーメッセージ |
|------|------------|----------------|
| メッセージ内容 | 空白でないこと | メッセージを入力してください。 |
| メッセージ長 | 1〜2000文字 | メッセージは2000文字以内で入力してください。 |
| 送信間隔 | 前回送信から1秒以上経過 | 送信が多すぎます。少し待ってから再度送信してください。 |

### API 連携

| 項目 | チェック内容 | エラーメッセージ / 処理内容 |
|------|------------|--------------------------|
| ユーザー認証 | ログイン済みユーザー | 認証されていない場合はログイン画面へリダイレクト |
| API キー | 有効な ChatGPT API キー | バックエンド側で環境変数から取得、設定不備時はエラーログ記録 |
| API レート制限 | API 呼び出し上限確認 | 上限超過時は「一時的にご利用いただけません」メッセージ表示 |
| トークン上限 | 会話ログの総トークン数 | 上限値（例: 4000トークン）に達した場合、新規会話の提案 |

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller | `ChatAssistantController` | `org.example.features.ChatAssistant.controller` |
| Service | `ChatAssistantService` | `org.example.features.ChatAssistant.service` |
| Service 実装 | `ChatAssistantServiceImpl` | `org.example.features.ChatAssistant.service.impl` |
| DTO（チャットメッセージ） | `ChatMessage` | `org.example.features.ChatAssistant.dto` |
| DTO（会話） | `Conversation` | `org.example.features.ChatAssistant.dto` |
| DTO（API リクエスト） | `ChatGPTRequestDto` | `org.example.features.ChatAssistant.dto` |
| DTO（API レスポンス） | `ChatGPTResponseDto` | `org.example.features.ChatAssistant.dto` |
| Entity | `ChatHistory` | `org.example.entity` |
| Entity | `UserConversation` | `org.example.entity` |
| Repository | `ChatHistoryRepository` | `org.example.repository` |
| Repository | `UserConversationRepository` | `org.example.repository` |
| External API Client | `ChatGPTApiClient` | `org.example.external.openai` |
| Utility | `TokenCounterUtil` | `org.example.util` |
| Configuration | `ChatAssistantConfig` | `org.example.config` |

---

*作成日: 2026-10-08*