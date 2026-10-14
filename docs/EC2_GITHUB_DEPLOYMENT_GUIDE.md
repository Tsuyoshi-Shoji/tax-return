# EC2 + GitHub Actions デプロイガイド

> 私用端末限定の運用では [WireGuard + HTTPS手順](PRIVATE-ACCESS.md) を優先する。アプリ用の公開ポートは開けず、VPNと制限された管理経路だけを使用する。

このガイドでは、AWS EC2 + EBS + GitHub Actions を使用した本番環境への自動デプロイを構築します。

## 前提条件

- AWS アカウント
- EC2 インスタンス（Amazon Linux 2 または 2023）
- EBS ボリューム（MySQL データ用）
- GitHub リポジトリの管理権限

## フェーズ 1: EC2 インスタンスの作成と初期化

### Step 1-1: EC2 インスタンスの起動

1. **AWS Management Console** → **EC2** → **インスタンスを起動**

2. **イメージ選択**: Amazon Linux 2 (2.0.xxxxx) または Amazon Linux 2023

3. **インスタンスタイプ**: `t3.medium` 以上推奨

4. **ネットワーク設定**:
   - VPC: デフォルト
   - 自動割り当てパブリック IP: 有効
   - セキュリティグループ: 新規作成
     - インバウンドルール:
       - SSH (22): 自分の IP のみ
       - Tomcat (8080): 必要な送信元のみ
         - IPv4: `0.0.0.0/0` または限定IP
         - IPv6: `::/0` または限定IPv6

5. **ストレージ設定**: 最小 30GB

6. **キーペア**: 新規作成し、`.pem` ファイルをダウンロード・保存

### Step 1-2: EBS ボリュームの作成とマウント

1. **新しい EBS ボリュームを作成**
   - サイズ: 最小 100GB
   - AZ: EC2 インスタンスと同じ
   - 暗号化: 有効（推奨）

2. **EC2 インスタンスにアタッチ**
   - Device name: `/dev/sdf` (設定後は `/dev/nvme1n1` として認識)

3. **SSH でインスタンスに接続**
   ```bash
   ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>
   ```

4. **ボリュームをフォーマット・マウント**
   ```bash
   # ボリュームの確認
   lsblk
   
   # フォーマット（データ損失注意！）
   sudo mkfs -t ext4 /dev/nvme1n1
   
   # マウントディレクトリ作成
   sudo mkdir -p /data/mysql
   
   # マウント
   sudo mount /dev/nvme1n1 /data/mysql
   
   # マウント確認
   df -h
   ```

5. **永続マウント設定** (`/etc/fstab`)
   ```bash
   sudo bash -c 'echo "/dev/nvme1n1 /data/mysql ext4 defaults,nofail 0 2" >> /etc/fstab'
   
   # 検証
   sudo mount -a
   ```

### Step 1-3: セットアップスクリプトの実行

1. **リポジトリをクローン**
   ```bash
   git clone https://github.com/Tsuyoshi-Shoji/tax-return.git
   cd tax-return
   ```

2. **セットアップスクリプト実行**
   ```bash
   chmod +x scripts/setup-ec2.sh
   ./scripts/setup-ec2.sh
   ```

   このスクリプトは以下をセットアップします：
   - Java 21（Amazon Corretto）
   - Tomcat 11.0.21
   - MySQL 8.0（EBS マウント）
   - 自動バックアップ（毎日 03:00）
   - ファイアウォール設定

3. **出力例**
   ```
   ✅ Setup Complete!
   
   Database Credentials saved to: /opt/db-credentials.env
   MySQL User: taxapp
   MySQL Database: taxtax
   ```

## フェーズ 2: GitHub Actions の設定

### Step 2-1: GitHub Secrets を設定

1. **リポジトリの Settings へアクセス**
   - `https://github.com/Tsuyoshi-Shoji/tax-return/settings`

2. **Secrets and variables** → **Actions** をクリック

3. **New repository secret** で以下を追加：

   **Secret 1: `EC2_HOST`**
   - Name: `EC2_HOST`
   - Value: EC2 インスタンスのパブリック IP / IPv6 / ホスト名
   - IPv6 を登録する場合は角括弧なしで入力
     ```
     54.123.45.67
     ```

   **Secret 2: `EC2_PRIVATE_KEY`**
   - Name: `EC2_PRIVATE_KEY`
   - Value: ダウンロードした `.pem` ファイルの全文
     ```
     -----BEGIN RSA PRIVATE KEY-----
     MIIEpAIBAAKCAQEA...
     ...
     -----END RSA PRIVATE KEY-----
     ```

### Step 2-2: main ブランチの保護設定（オプション）

1. **Settings** → **Branches** → **Add rule**

2. **Branch name pattern**: `main`

3. **Require status checks to pass**: チェック
   - `build-and-deploy` を追加

4. **Dismiss stale pull request approvals**: チェック（推奨）

## フェーズ 3: デプロイのテスト

### Step 3-1: 初回デプロイ実行

1. **任意の変更を main ブランチに push**
   ```bash
   git add .
   git commit -m "Initial deployment setup"
   git push origin main
   ```

2. **GitHub Actions で実行を確認**
   - `https://github.com/Tsuyoshi-Shoji/tax-return/actions`

3. **ワークフロー実行状況を監視**
   - ✅ Build WAR
   - ✅ Deploy via SSH
   - ✅ Verify Deployment

### Step 3-2: 本番環境へのアクセス確認

1. **アプリケーションが起動したか確認**
   ```bash
   # IPv6 の場合は curl http://[<EC2_HOST>]:8080/final-tax-return-1.0-SNAPSHOT/
   curl http://<EC2_HOST>:8080/final-tax-return-1.0-SNAPSHOT/
   ```

2. **ログで詳細を確認**
   ```bash
   ssh -i your-key.pem ec2-user@<EC2_HOST>
   
   # デプロイログ
   tail -f /var/log/deploy.log
   
   # Tomcat ログ
   tail -f /opt/tomcat/logs/catalina.out
   ```

## 日常的な運用

### デプロイ手順

```bash
# ローカルで機能開発
git checkout -b feature/xxx
# ... コード変更 ...
git add .
git commit -m "feat: xxx"
git push origin feature/xxx

# GitHub で Pull Request を作成
# レビュー後、main にマージ

# 自動的にデプロイ実行
# Actions ページで進行状況を確認
```

### ロールバック方法

1. **前のコミットに戻す（最も安全）**
   ```bash
   git revert <commit-hash>
   git push origin main
   ```
   → 自動デプロイが実行され、前の版が復元される

2. **EC2 インスタンス上でロールバック**
   ```bash
   ssh -i your-key.pem ec2-user@<EC2_HOST>
   
   # バックアップ済み WAR を確認
   ls -la /opt/backups/war/
   
   # 前の版をリストア
   cp /opt/backups/war/final-tax-return-1.0-SNAPSHOT.war.20240115-150000.bak \
      /opt/tomcat/webapps/final-tax-return-1.0-SNAPSHOT.war
   
   # Tomcat 再起動
   sudo systemctl restart tomcat
   ```

### 監視項目

| 項目 | 確認コマンド | 頻度 |
|------|-------------|------|
| デプロイログ | `tail -f /var/log/deploy.log` | デプロイ後 |
| アプリログ | `tail -f /opt/tomcat/logs/catalina.out` | 日 1 回 |
| MySQL ログ | `tail -f /var/log/mysql/error.log` | 日 1 回 |
| バックアップ | `ls -la /data/mysql/backups/` | 週 1 回 |
| ディスク使用量 | `df -h` | 月 1 回 |

## セキュリティ考慮事項

詳細は `docs/SECURITY_DEPLOYMENT.md` を参照してください。

- ✅ 秘密鍵は GitHub Secrets で管理
- ✅ EC2 セキュリティグループで SSH アクセスを制限
- ✅ MySQL は localhost のみでリッスン
- ✅ 自動バックアップ（7 日保有）
- ✅ デプロイログにパスワード記録なし

## トラブルシューティング

### デプロイが失敗する場合

```bash
# EC2 にアクセス
ssh -i your-key.pem ec2-user@<EC2_HOST>

# デプロイログ確認
cat /var/log/deploy.log

# Maven ビルドエラー
cd /home/ec2-user/tax-return
mvn clean package -DskipTests

# Tomcat ログ確認
tail -100 /opt/tomcat/logs/catalina.out
```

### アプリケーションが起動しない場合

```bash
# Tomcat プロセス確認
ps aux | grep tomcat

# ポート確認
sudo netstat -lntp | grep 8080

# MySQL 接続確認
source /opt/db-credentials.env
mysql -u "$DB_USER" -p"$DB_PASS" -h localhost "$DB_NAME" -e "SELECT 1;"
```

## サポート

問題が発生した場合：

1. [SECURITY_DEPLOYMENT.md](./SECURITY_DEPLOYMENT.md) のトラブルシューティングセクションを確認
2. GitHub Actions のログを確認
3. EC2 インスタンスのログを確認
   - `/var/log/deploy.log`
   - `/opt/tomcat/logs/catalina.out`
   - `/var/log/mysql/error.log`
