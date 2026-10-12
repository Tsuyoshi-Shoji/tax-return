# EC2 + MySQL + EBS デプロイメント ガイド

このドキュメントは、tax-return アプリケーションを AWS EC2 に配置し、MySQL をデータベース、EBS をストレージとして使用する構成のデプロイメント手順です。

## アーキテクチャ

```
GitHub (main branch)
    ↓
GitHub Actions (CI/CD)
    ├─ Maven build & test
    └─ SSH deploy
        ↓
EC2 Instance (Amazon Linux 2)
├─ Tomcat 11
├─ Java 17
├─ MySQL (EBS /data/mysql)
└─ Automated backups (daily 03:00)
```

## 前提条件

- AWS アカウント
- EC2 インスタンス（t3.medium 以上推奨）
- EBS ボリューム（アタッチ済み）
- GitHub リポジトリへのアクセス
- EC2 キーペア

---

## ステップ 1: AWS インフラ構築

### 1.1 EC2 インスタンス起動

```bash
# EC2 Dashboard から以下の設定で起動
- AMI: Amazon Linux 2
- Instance Type: t3.medium (2 vCPU, 4GB RAM)
- Root Volume: 30GB (gp3)
- Security Group: 
  * Inbound: 22 (SSH), 80 (HTTP), 443 (HTTPS) from your IP
  * Outbound: All
```

### 1.2 EBS ボリューム追加・アタッチ

EC2 コンソールから EBS ボリュームを新規作成し、インスタンスにアタッチします。

```bash
# EC2 内でマウント
sudo mkfs -t ext4 /dev/nvme1n1
sudo mkdir -p /data/mysql
sudo mount /dev/nvme1n1 /data/mysql

# 永続マウント設定
echo '/dev/nvme1n1 /data/mysql ext4 defaults,nofail 0 2' | sudo tee -a /etc/fstab
```

### 1.3 EBS 暗号化・バックアップ設定

- EBS ボリュームの暗号化を有効化（デフォルト推奨）
- CloudWatch イベント + Lambda でスナップショット自動化（オプション）

---

## ステップ 2: EC2 環境セットアップ

### 2.1 セットアップスクリプト実行

EC2 インスタンスに接続し、以下を実行します：

```bash
# リポジトリをクローン
git clone https://github.com/Tsuyoshi-Shoji/tax-return.git
cd tax-return

# セットアップスクリプト実行
bash scripts/setup-ec2.sh
```

このスクリプトは以下を自動実行します：

- ✅ Java 17 + Tomcat 11 インストール
- ✅ MySQL インストール・起動
- ✅ MySQL データベース・ユーザー作成
- ✅ Tomcat 環境設定
- ✅ デプロイスクリプト配置
- ✅ バックアップスクリプト設定
- ✅ ファイアウォール設定

### 2.2 セットアップ確認

```bash
# MySQL が起動しているか確認
sudo systemctl status mysqld

# Tomcat ディレクトリ確認
ls -la /opt/tomcat

# 認証情報ファイル確認
sudo cat /opt/db-credentials.env
```

---

## ステップ 3: GitHub Actions 設定

### 3.1 GitHub Secrets 登録

GitHub リポジトリ Settings → Secrets に以下を登録：

| Secret Name | Value |
|---|---|
| `EC2_HOST` | EC2 インスタンスのパブリック IP / IPv6 / ホスト名 |
| `EC2_PRIVATE_KEY` | EC2 キーペア (.pem ファイルの内容全文) |

**キーペアの取得方法：**
```bash
# ローカルマシンで実行
cat ~/.ssh/your-key-pair.pem
# 出力をコピーして GitHub Secrets に貼り付け
```

### 3.2 ワークフロー確認

`.github/workflows/deploy-ec2.yml` がリポジトリに含まれていることを確認します。

```bash
cat .github/workflows/deploy-ec2.yml
```

---

## ステップ 4: デプロイメント実行

### 4.1 初回デプロイ

`main` ブランチにコミット・プッシュします：

```bash
git add .
git commit -m "Initial EC2 deployment setup"
git push origin main
```

GitHub Actions が自動実行されます。

**ワークフロー実行確認：**
- GitHub リポジトリ → Actions タブ
- 最新のワークフロー実行状況を確認

### 4.2 デプロイ状況監視

EC2 インスタンスで以下のログを監視：

```bash
# デプロイログ
tail -f /var/log/deploy.log

# Tomcat ログ
tail -f /opt/tomcat/logs/catalina.out

# MySQL ログ
tail -f /var/log/mysql/error.log
```

### 4.3 ヘルスチェック

```bash
# EC2 インスタンスから
curl http://localhost:8080/final-tax-return-1.0-SNAPSHOT/home

# ローカルから
# IPv6 の場合は curl http://[<EC2_HOST>]:8080/final-tax-return-1.0-SNAPSHOT/home
curl http://<EC2_HOST>:8080/final-tax-return-1.0-SNAPSHOT/home
```

---

## ステップ 5: 本番運用

### 5.1 バックアップ確認

```bash
# 自動バックアップ状況確認
ls -la /data/mysql/backups/

# 手動バックアップ実行
sudo /opt/backup-mysql.sh
```

### 5.2 Tomcat 起動・停止

```bash
# Tomcat 起動
sudo systemctl start tomcat

# Tomcat 停止
sudo systemctl stop tomcat

# ステータス確認
sudo systemctl status tomcat
```

### 5.3 MySQL 管理

```bash
# MySQL に接続
mysql -u taxapp -p -h localhost

# パスワード確認
sudo cat /opt/db-credentials.env

# データベースサイズ確認
SELECT table_name, round(((data_length + index_length) / 1024 / 1024), 2) AS size_mb 
FROM information_schema.tables WHERE table_schema = 'taxtax';
```

### 5.4 ディスク使用量監視

```bash
# ディスク使用状況
df -h /data/mysql

# MySQL データベースサイズ
du -sh /data/mysql

# Tomcat ログサイズ
du -sh /opt/tomcat/logs
```

---

## トラブルシューティング

### Q: Tomcat が起動しない

```bash
# ログを確認
tail -f /opt/tomcat/logs/catalina.out

# Java プロセス確認
ps aux | grep java

# ポート確認
netstat -tlnp | grep 8080
```

### Q: MySQL に接続できない

```bash
# MySQL ステータス確認
sudo systemctl status mysqld

# 認証情報確認
sudo cat /opt/db-credentials.env

# 接続テスト
mysql -u taxapp -p -h localhost -e "SELECT 1"
```

### Q: デプロイが失敗する

```bash
# デプロイログ確認
tail -f /var/log/deploy.log

# Git ステータス確認
cd /home/ec2-user/tax-return
git status

# Maven ビルド手動実行
mvn clean package -DskipTests
```

### Q: EBS ボリュームマウント解除

```bash
# マウント状況確認
mount | grep /data/mysql

# アンマウント
sudo umount /data/mysql

# 再マウント
sudo mount /dev/nvme1n1 /data/mysql
```

---

## セキュリティ設定

### 6.1 ファイアウォール設定

```bash
# ポート確認
sudo firewall-cmd --list-all

# HTTP/HTTPS 許可
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https

# MySQL ポート不許可
sudo firewall-cmd --permanent --remove-port=3306/tcp

# 設定リロード
sudo firewall-cmd --reload
```

### 6.2 SSH キー管理

```bash
# EC2 インスタンスキーペア保護
chmod 600 ~/.ssh/your-key-pair.pem

# GitHub Actions で使用中
# -> GitHub Secrets に安全に保管
```

### 6.3 データベースセキュリティ

```bash
# リモート接続無効化確認
mysql -u root -p -e "SELECT Host FROM mysql.user WHERE User='root';"
# localhost のみ表示されるべき

# アプリケーションユーザー権限確認
mysql -u root -p -e "SHOW GRANTS FOR 'taxapp'@'localhost';"
```

---

## パフォーマンスチューニング

### 7.1 MySQL チューニング

```bash
# /etc/my.cnf.d/mysql-custom.cnf で以下を設定
[mysqld]
innodb_buffer_pool_size=512M  # インスタンスサイズに応じて調整
max_connections=150
slow_query_log=1
```

### 7.2 Tomcat チューニング

```bash
# /opt/tomcat/bin/setenv.sh
export JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC"
# インスタンスメモリに応じて調整
```

### 7.3 EBS スループット最適化

```bash
# EBS ボリュームプロパティ
# - Volume Type: gp3 推奨
# - IOPS: 3000
# - Throughput: 125 MB/s
```

---

## モニタリング・ロギング

### 8.1 CloudWatch ログ出力

```bash
# Tomcat ログを CloudWatch に送信
# CloudWatch Logs Agent をインストール・設定
```

### 8.2 アラート設定

- CPU 使用率が 80% 以上
- ディスク使用率が 80% 以上
- MySQL コネクション数が上限に近い

```bash
# EC2 → CloudWatch → Alarms で設定
```

---

## 定期メンテナンス

### 9.1 週次チェック

```bash
# ディスク使用率確認
df -h

# MySQL スロークエリログ確認
tail -f /var/log/mysql/slow-query.log

# Tomcat ログファイルサイズ確認
ls -lh /opt/tomcat/logs/
```

### 9.2 月次メンテナンス

```bash
# バックアップファイル確認
ls -lh /data/mysql/backups/

# 古いログファイル削除
find /opt/tomcat/logs -name "*.log" -mtime +30 -delete

# MySQL テーブル最適化
mysql -u taxapp -p taxtax -e "OPTIMIZE TABLE \`*\`;"
```

### 9.3 復旧テスト（3ヶ月ごと）

```bash
# 最新バックアップから復旧
# テスト用インスタンスで検証
mysql -u root -p < /data/mysql/backups/backup-latest.sql.gz
```

---

## コスト最適化

- **EC2**: 予約インスタンスで割引
- **EBS**: gp3 で gp2 より 20% コスト削減
- **スナップショット**: 古いスナップショット自動削除

---

## 困った時の連絡先・参考資料

- **AWS EC2 ドキュメント**: https://docs.aws.amazon.com/ec2/
- **Tomcat ドキュメント**: https://tomcat.apache.org/
- **MySQL ドキュメント**: https://dev.mysql.com/doc/
- **GitHub Actions**: https://docs.github.com/en/actions

---

**最終更新**: 2026-10-11
