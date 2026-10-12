# EC2 + GitHub Actions CI/CD デプロイ実装完了報告

**実装日**: 2026-10-12
**対象ブランチ**: main
**初回デプロイ対象**: 本番環境（単一 EC2 インスタンス）

## ✅ 実装完了項目

### 1. GitHub Actions ワークフロー改善

**ファイル**: `.github/workflows/deploy-ec2.yml`

**改善内容**:
- ✅ Java バージョン統一: `17` → `21` (pom.xml に合わせ)
- ✅ ビルドキャッシュ: `cache: maven` を追加（CI 時間短縮）
- ✅ テストステップ削除: 未実装のテストスキップを正式化
- ✅ WAR 検証ステップ追加: ビルド成果物の確認
- ✅ デプロイタイムアウト設定: `180s` で長時間実行に対応
- ✅ ヘルスチェック強化: リトライロジック改善（10 回、3 秒間隔）
- ✅ デプロイサマリー: 結果通知の見易さ向上

**トリガー条件**:
```yaml
on:
  push:
    branches: [ main ]
```
→ main ブランチへのプッシュで自動実行

### 2. デプロイスクリプト改善

**ファイル**: `scripts/deploy.sh`

**改善内容**:
- ✅ エラーハンドリング: `set -euo pipefail` で厳密なエラー検出
- ✅ ロールバック機能: 前のバージョンを `/opt/backups/war/` に自動保存（最新 5 世代保有）
- ✅ ログ出力: 全ステップをマーク付きで記録（日時、セクション分け）
- ✅ セキュリティ: コミット SHA、実行時刻をログに記録（パスワードは記録しない）
- ✅ Tomcat 管理: より確実な停止・起動（プロセス強制終了、待機時間調整）
- ✅ ヘルスチェック: 30 回リトライ（最大 60 秒）

**実行フロー**:
```
1. リポジトリ更新 (git fetch/reset)
2. Maven ビルド
3. 旧バージョンをバックアップ
4. Tomcat 停止
5. 旧デプロイをクリーン
6. 新 WAR デプロイ
7. Tomcat 起動
8. ヘルスチェック
```

### 3. EC2 初期化スクリプト改善

**ファイル**: `scripts/setup-ec2.sh`

**改善内容**:
- ✅ EBS 20GiB 前提: 初回セットアップ時に自動マウント
- ✅ 依存関係統合: `dnf install` を 1 回にまとめる（実行時間短縮）
- ✅ Java バージョン: `21` に統一
- ✅ エラーハンドリング: `set -euo pipefail`
- ✅ 秘密情報管理: パーミッション `600` (root のみ読み取り可能)
- ✅ setenv.sh 検証: 環境変数ファイル欠落時にエラー終了
- ✅ バックアップディレクトリ作成: `/opt/backups/war` を事前準備
- ✅ MySQL: ランダムパスワード生成（OpenSSL 使用）
- ✅ Firewall: ポート 3306 明示的に閉鎖
- ✅ Cron 設定: 自動バックアップ（毎日 03:00）
- ✅ メモリ最適化: t3.micro 向けに Java / MySQL の使用量を抑制

**セットアップ所要時間**: 約 10-15 分

### 4. ドキュメント整備

#### 4-1. GitHub Secrets セットアップガイド
**ファイル**: `docs/GITHUB_SECRETS_SETUP.md`

- GitHub Secrets の登録方法（スクリーンショット付き）
- 必須 2 つの Secrets: `EC2_HOST`, `EC2_PRIVATE_KEY`
- トラブルシューティング（認証エラー、ホスト接続失敗など）

#### 4-2. セキュリティ・運用ガイド
**ファイル**: `docs/SECURITY_DEPLOYMENT.md`

- 秘密情報管理（GitHub Secrets vs. EC2 環境変数）
- ログマスキング戦略
- アクセス制御（GitHub、EC2、MySQL）
- バックアップ・復旧手順
- ロールバック方法（2 パターン）
- 監視項目とインシデント対応
- 定期メンテナンススケジュール

#### 4-3. デプロイ実行ガイド
**ファイル**: `docs/EC2_GITHUB_DEPLOYMENT_GUIDE.md`

- EC2 インスタンス作成（セキュリティグループ設定）
- EBS マウント手順（永続化設定を含む）
- setup-ec2.sh の実行方法
- GitHub Secrets 登録ステップバイステップ
- 初回デプロイテスト
- 日常的な運用（デプロイ、ロールバック）
- トラブルシューティング

## 📋 今後の初期設定手順

### フェーズ A: AWS 環境準備（1-2 時間）

```bash
# 1. EC2 インスタンス起動
#   - イメージ: Amazon Linux 2023
#   - インスタンスタイプ: t3.micro
#   - キーペアダウンロード

# 2. EBS ボリューム作成・アタッチ
#   - サイズ: 20GiB
#   - 暗号化: 有効

# 3. SSH 接続確認
ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>

# 4. EBS マウント・フォーマット
EBS_DEVICE=/dev/xvdf ./scripts/setup-ec2.sh
```

### フェーズ B: EC2 セットアップ（15 分）

```bash
# リポジトリクローン
git clone https://github.com/Tsuyoshi-Shoji/tax-return.git
cd tax-return

# セットアップスクリプト実行
chmod +x scripts/setup-ec2.sh
./scripts/setup-ec2.sh

# 出力から DB 認証情報を確認
# → /opt/db-credentials.env に保存される
```

### フェーズ C: GitHub Secrets 設定（5 分）

```
1. GitHub リポジトリの Settings へ移動
2. Secrets and variables → Actions
3. New repository secret
   - EC2_HOST: <EC2_PUBLIC_IP / IPv6 / ホスト名>
   - EC2_PRIVATE_KEY: <秘密鍵の全文>
```

### フェーズ D: 初回デプロイテスト（10-20 分）

```bash
# リポジトリに変更をコミット
git add .
git commit -m "chore: CI/CD デプロイ構築完了"
git push origin main

# GitHub Actions で実行確認
# → https://github.com/Tsuyoshi-Shoji/tax-return/actions

# EC2 インスタンスでログ監視
ssh -i your-key.pem ec2-user@<EC2_HOST>
tail -f /var/log/deploy.log

# アプリケーション起動確認
# IPv6 の場合は curl http://[<EC2_HOST>]:8080/final-tax-return-1.0-SNAPSHOT/
curl http://<EC2_HOST>:8080/final-tax-return-1.0-SNAPSHOT/
```

## 🔒 セキュリティ設定チェックリスト

- [ ] EC2 セキュリティグループで SSH アクセスを制限（自分の IP のみ）
- [ ] HTTP ポート 8080 を必要な IP のみに制限
- [ ] MySQL ポート 3306 は localhost のみでリッスン
- [ ] GitHub Secrets が暗号化されている（GitHub 管理）
- [ ] `/opt/db-credentials.env` のパーミッションが `600`
- [ ] ファイアウォール設定で MySQL を外部公開しない
- [ ] SSH キー（秘密鍵）をリポジトリに含めない
- [ ] 初回セットアップ後、ランダムパスワードを確認・保管

## 📊 主要ファイル構成

```
tax-return/
├── .github/workflows/
│   └── deploy-ec2.yml           # ✅ 改善版 (Java 21, セキュリティ強化)
│
├── scripts/
│   ├── setup-ec2.sh             # ✅ 改善版 (Java 21, 権限管理)
│   ├── deploy.sh                # ✅ 改善版 (ロールバック対応)
│   ├── backup-mysql.sh          # MySQL 自動バックアップ
│   └── health-check.sh          # ヘルスチェック
│
├── docs/
│   ├── EC2_GITHUB_DEPLOYMENT_GUIDE.md   # ✅ NEW (実装ガイド)
│   ├── GITHUB_SECRETS_SETUP.md          # ✅ NEW (Secrets 設定)
│   ├── SECURITY_DEPLOYMENT.md           # ✅ NEW (セキュリティ運用)
│   └── ...
│
└── src/main/resources/
    └── application-production.properties # 本番設定テンプレート
```

## 🚀 デプロイ実行例

### 新機能をリリース

```bash
# ローカルで開発
git checkout -b feature/new-feature
# ... コード変更 ...
git commit -m "feat: 新機能追加"
git push origin feature/new-feature

# GitHub でプルリクエスト作成 → レビュー → マージ

# main へのマージが完了すると自動デプロイ開始
# → Actions ページでプログレス確認
# → 約 3-5 分で本番環境に反映
```

### 本番環境で障害が発生

```bash
# Option 1: Git でロールバック（推奨）
git revert <失敗したコミットハッシュ>
git push origin main
# → 自動的に前の版が本番環境に復元

# Option 2: EC2 上で手動ロールバック
ssh -i your-key.pem ec2-user@<EC2_HOST>
ls -la /opt/backups/war/
cp /opt/backups/war/final-tax-return-1.0-SNAPSHOT.war.20240115-150000.bak \
   /opt/tomcat/webapps/final-tax-return-1.0-SNAPSHOT.war
sudo systemctl restart tomcat
```

## 📈 今後の拡張（オプション）

- [ ] **SSL/TLS 対応** (`application-production.properties` にコメント例あり)
- [ ] **複数環境対応** (ステージング、本番の分離)
- [ ] **ユニット・統合テスト実装** (テスト失敗時にデプロイ中止)
- [ ] **CloudWatch/監視アラート** (デプロイ失敗時の通知)
- [ ] **ブルーグリーンデプロイ** (ダウンタイムなし切り替え)
- [ ] **自動スケーリング** (ロードバランサー + 複数インスタンス)

## 📞 サポート

問題が発生した場合：

1. **docs/EC2_GITHUB_DEPLOYMENT_GUIDE.md** のトラブルシューティング確認
2. **GitHub Actions ログ** で失敗ステップを特定
3. **EC2 インスタンスのログ**を確認
   ```bash
   tail -f /var/log/deploy.log
   tail -f /opt/tomcat/logs/catalina.out
   tail -f /var/log/mysql/error.log
   ```

---

**実装完了日**: 2026-10-12  
**対応者**: Copilot  
**Co-authored-by**: Copilot <223556219+Copilot@users.noreply.github.com>
