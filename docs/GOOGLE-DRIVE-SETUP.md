# Google Drive 証憑保管の設定手順

確定申告画面の「年分別保存管理台帳」で、領収書・請求書等の証憑ファイル（PDF／JPEG／PNG、1件10 MB以下）を利用者本人の Google Drive に保管する機能の設定手順。設定が揃わない場合、機能は無効のまま表示され、メタデータ台帳は従来どおり使える。

> **現在は停止中（2026-11-13〜）**: 将来提供予定の機能として、`EVIDENCE_UPLOAD_ENABLED=true` を設定しない限り停止している（既定値 `false`）。停止中は連携・アップロード・ダウンロード・Drive確認の画面表示を出さず、サーバーも操作を拒否する。保存管理台帳の登録・検索・期限管理・保全・ファイルのない記録の削除は利用できる。過去に保管したファイルがある記録は、停止中は削除を拒否する（Drive 上のファイルを残さないため）。再開時は下記手順の設定に加えて `EVIDENCE_UPLOAD_ENABLED=true` を設定し、`EvidenceFileHarness` を実行してから再起動する。

## 仕組みと送信データ

| 項目 | 内容 |
| --- | --- |
| 接続先 | Google OAuth 2.0（`accounts.google.com` / `oauth2.googleapis.com`）、Google Drive API v3（`www.googleapis.com`）。宛先はコードに固定し、利用者指定URLは取得しない |
| 認可方式 | 利用者ごとの OAuth 2.0 認可コード＋PKCE（S256）＋state。スコープは `https://www.googleapis.com/auth/drive.file` のみ（アプリが作成したファイルだけにアクセス） |
| 送信項目 | 証憑ファイル本体、サーバー生成のファイル名 `evidence-{台帳ID}-{UUID}.{拡張子}`、MIME種別、保管フォルダー名「確定申告アプリ 証憑」。氏名・元のファイル名・金額・仕訳内容は送信しない |
| DB保存 | 更新トークン（AES-256-GCMで暗号化）、フォルダーID、ファイルID、元ファイル名（表示用に無害化）、MIME、サイズ、SHA-256、検証状態。アクセストークンは保存しない |
| 保持・削除 | Drive上のファイルは台帳の保存期限経過後、アプリの「削除」で完全削除（ゴミ箱を経由しない）。連携解除ではトークンを失効・削除するが、Drive上のファイルは利用者の管理として残る |
| データ所在地・学習利用 | Google アカウント（利用者本人）の Drive 契約・設定に従う。アプリから学習利用や所在地を保証しない。利用前に利用者自身が Google の規約を確認する |

## 1. Google Cloud プロジェクトの作成

1. [Google Cloud Console](https://console.cloud.google.com/) で専用プロジェクトを作成する（他用途と共用しない）。
2. 「APIとサービス」→「ライブラリ」で **Google Drive API** を有効化する。

## 2. OAuth 同意画面

1. 「Google Auth Platform」（旧「OAuth 同意画面」）でアプリ名・サポートメールを設定する。
2. 対象は「外部」とし、公開ステータスを「テスト」のままにする場合はテストユーザーに利用者の Google アカウントを追加する。
   - テスト状態では更新トークンが7日で失効する場合がある。失効時は画面で再連携を求める。長期運用では公開設定と Google の審査要件を確認する。
3. スコープは `.../auth/drive.file` のみを追加する。`drive` や `drive.readonly` 等の広いスコープは追加しない（アプリは `drive.file` がない許可を拒否・失効する）。

## 3. OAuth クライアントの作成

1. 「認証情報」→「OAuth クライアント ID を作成」→ 種類「ウェブ アプリケーション」。
2. 承認済みのリダイレクト URI に、アプリのコンテキストパスを含む次のURLを**完全一致**で登録する。

   ```
   https://<ホスト名>/<コンテキストパス>/tax-return/evidence/google/callback
   ```

   ローカル確認時のみ `http://localhost:<ポート>/<コンテキストパス>/tax-return/evidence/google/callback` を使用できる。それ以外の `http://` はアプリ側で無効扱いとなる。
3. 発行されたクライアントID・クライアントシークレットを控える（リポジトリ、チケット、チャットへ貼らない）。

## 4. 暗号鍵の生成

更新トークン暗号化用に32バイトの乱数を Base64 で生成する。

```bash
openssl rand -base64 32
```

鍵を紛失すると保存済みトークンを復号できず、全利用者の再連携が必要になる（Drive上のファイルは失われない）。鍵はDBバックアップと別の場所に保管する。

## 5. 環境変数の設定

アプリは次の環境変数（またはシステムプロパティ）だけから設定を読み込む。properties ファイルへ秘密値を記載しない。

| 変数 | 内容 |
| --- | --- |
| `EVIDENCE_UPLOAD_ENABLED` | `true` で機能を有効化（未設定・`true` 以外は停止） |
| `GOOGLE_OAUTH_CLIENT_ID` | OAuth クライアントID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | OAuth クライアントシークレット |
| `GOOGLE_OAUTH_REDIRECT_URI` | 手順3で登録したリダイレクトURI（完全一致） |
| `EVIDENCE_TOKEN_KEY` | 手順4の Base64 鍵（復号後32バイト） |

EC2／Tomcat では `/opt/evidence-drive.env` を作成する。`scripts/tomcat-setenv.sh` はこのファイルが読める場合のみ読み込み、変数をJavaプロセスへ渡す（ファイルがなければ機能は無効）。

```bash
sudo install -o root -g tomcat -m 640 /dev/null /opt/evidence-drive.env
sudo editor /opt/evidence-drive.env
# EVIDENCE_UPLOAD_ENABLED=true
# GOOGLE_OAUTH_CLIENT_ID=...
# GOOGLE_OAUTH_CLIENT_SECRET=...
# GOOGLE_OAUTH_REDIRECT_URI=https://example.com/app/tax-return/evidence/google/callback
# EVIDENCE_TOKEN_KEY=...
sudo systemctl restart tomcat
```

## 6. 動作確認

1. 確定申告画面の保存管理台帳で「Google Drive と連携」を押し、Google の同意画面で許可する。
2. 台帳記録に PDF/JPEG/PNG をアップロードし、「確認」で Drive 上の存在・サイズを確認する。
3. ダウンロード時にSHA-256が一致することを確認する（不一致時は登録時と一致しない旨を表示してダウンロードしない）。

## 運用

- **鍵・シークレットの更新**: クライアントシークレットは Cloud Console で追加発行→環境変数更新→再起動→旧シークレット削除の順で切り替える。`EVIDENCE_TOKEN_KEY` を変更すると保存済みトークンは復号できないため、各利用者に再連携を案内する（再暗号化ツールは未実装）。
- **漏えい時**: クライアントシークレットを無効化・再発行し、`EVIDENCE_TOKEN_KEY` を更新して `evidence_drive_connection` の行を削除する。利用者には Google アカウントの「サードパーティ アクセス」から本アプリのアクセス削除も案内する。
- **監査ログ**: ロガー `audit.evidence` に操作・利用者ID・台帳ID・結果のみ記録する。トークン、ファイル名、ファイル内容は記録しない。

## 制限事項・残余リスク

- マルウェア検査は未実装（下記の例外記録を参照）。形式・拡張子・マジックバイト・サイズ検証と、PDF内の JavaScript／Launch／埋め込みファイル／RichMedia の拒否、ダウンロード時の `attachment`・`nosniff`・`CSP sandbox` で代替する。
- 利用者が Drive 上で直接削除・ゴミ箱移動・差替えした場合、アプリは「確認」またはダウンロード時のハッシュ照合で検知するだけで、防止・復元はできない。
- 電子帳簿保存法（タイムスタンプ、訂正削除履歴、検索要件等）への適合は判定・表示しない。
- 誤ってアップロードしたファイルは保存期限経過前に削除できない（期限前の削除防止を優先）。
- Google の障害時はアップロード・ダウンロードできない。メタデータ台帳と手動保管で継続する。

### セキュリティ例外記録（マルウェア検査）

| 項目 | 内容 |
| --- | --- |
| 対象 | 証憑ファイルアップロード（`POST /tax-return/retention/{id}/file`）のマルウェア検査・隔離 |
| 理由 | 個人利用・単一利用者の運用で、検査エンジンの導入・更新体制がない |
| 代替対策 | 許可形式3種・10 MB上限・マジックバイト照合・危険なPDF機能の拒否、サーバーで生成した保存名、アプリ内でファイルを実行・変換・プレビューしない、ダウンロード時の `Content-Disposition: attachment`・`X-Content-Type-Options: nosniff`・`Content-Security-Policy: default-src 'none'; sandbox` |
| 責任者／承認者 | 本人／本人 |
| 有効期限 | 2027-03-31（複数利用者への提供開始時は即時失効） |
| 再評価日 | 2026-12-31 |
