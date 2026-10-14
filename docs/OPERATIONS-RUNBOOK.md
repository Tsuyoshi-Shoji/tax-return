# Operational Runbook - EC2 + MySQL 本番運用

本番環境の各種操作手順を記載しています。

> 私用端末限定構成では [WireGuard + HTTPS手順](PRIVATE-ACCESS.md) を優先する。障害時もアプリの公開ポートを開けない。Tomcatの起動方式は実構成に従う（現在のデプロイスクリプトは `catalina.sh` を使用）。

## 緊急対応

### アプリケーション起動・停止

```bash
# Tomcat 起動
sudo systemctl start tomcat

# Tomcat 停止
sudo systemctl stop tomcat

# Tomcat 再起動
sudo systemctl restart tomcat

# ステータス確認
sudo systemctl status tomcat

# ログ確認
tail -f /opt/tomcat/logs/catalina.out
```

### MySQL 起動・停止

```bash
# MySQL 起動
sudo systemctl start mysqld

# MySQL 停止
sudo systemctl stop mysqld

# MySQL 再起動
sudo systemctl restart mysqld

# ステータス確認
sudo systemctl status mysqld

# ログ確認
tail -f /var/log/mysql/error.log
```

### 緊急復旧（全サービス再起動）

```bash
# MySQL 停止
sudo systemctl stop mysqld
sleep 5

# MySQL 起動
sudo systemctl start mysqld
sleep 10

# Tomcat 停止
sudo systemctl stop tomcat
sleep 5

# Tomcat 起動
sudo systemctl start tomcat

# ヘルスチェック
curl http://localhost:8080/final-tax-return-1.0-SNAPSHOT/home
```

---

## バックアップ・復旧

### 手動バックアップ実行

```bash
# 即座にバックアップを作成
sudo /opt/backup-mysql.sh

# バックアップ一覧表示
ls -lh /data/mysql/backups/
```

### バックアップから復旧

```bash
# 最新バックアップから復旧
sudo /opt/restore-mysql.sh

# 特定のバックアップから復旧
sudo /opt/restore-mysql.sh -f /data/mysql/backups/backup_20261011_030000.sql.gz

# 別のデータベースへ復旧（テスト用）
sudo /opt/restore-mysql.sh -d taxtax_test
```

### バックアップ検証

```bash
# バックアップファイルの完全性チェック
gzip -t /data/mysql/backups/backup_latest.sql.gz

# テスト復旧（本番データに影響なし）
# テスト用インスタンス、または別の MySQL サーバーで:
mysql -u root -p < backup_test.sql
```

---

## ディスク容量管理

### 容量確認

```bash
# EBS ボリューム全体
df -h /data/mysql

# MySQL データベースサイズ
du -sh /data/mysql

# バックアップサイズ
du -sh /data/mysql/backups/

# Tomcat ログサイズ
du -sh /opt/tomcat/logs/
```

### 容量逼迫時の対応

```bash
# ステップ 1: 古いバックアップ削除
find /data/mysql/backups -name "backup_*.sql.gz" -mtime +30 -delete

# ステップ 2: Tomcat ログ古いファイル削除
find /opt/tomcat/logs -name "*.log" -mtime +7 -delete

# ステップ 3: 一時ファイル削除
rm -rf /tmp/*

# ステップ 4: 再度確認
df -h /data/mysql
```

### EBS ボリュームサイズ拡張（AWS CLI）

```bash
# 現在のサイズ確認
aws ec2 describe-volumes --volume-ids vol-xxxxxxxxx \
  --query 'Volumes[0].Size' --output text

# ボリュームサイズ拡張（例: 200GB に拡張）
aws ec2 modify-volume --volume-id vol-xxxxxxxxx --size 200

# インスタンス内でファイルシステム拡張
sudo resize2fs /dev/nvme1n1

# 拡張完了確認
df -h /data/mysql
```

---

## パフォーマンス監視

### リアルタイムモニタリング

```bash
# メモリ・CPU 使用状況
top -b -n 1 | head -20

# メモリ詳細
free -h

# ディスク I/O
iostat -x 1 5

# ネットワーク I/O
iftop -n
```

### MySQL パフォーマンス

```bash
# MySQL に接続
mysql -u taxapp -p -h localhost

# 接続数確認
SHOW PROCESSLIST;

# スロークエリログ確認
SELECT * FROM mysql.slow_log;

# テーブルサイズ
SELECT table_name, round(((data_length + index_length) / 1024 / 1024), 2) AS size_mb 
FROM information_schema.tables 
WHERE table_schema = 'taxtax' 
ORDER BY size_mb DESC;

# インデックス確認
SHOW INDEXES FROM table_name;

# テーブル最適化
OPTIMIZE TABLE table_name;
```

### Tomcat パフォーマンス

```bash
# Tomcat メモリ使用量
ps aux | grep java | grep -v grep

# Tomcat スレッド数
echo "Current Threads: $(jcmd $(pgrep -f catalina) Thread.print | grep -c "tid")"

# ホットスポット（JIT コンパイル）
grep "made not entrant" /opt/tomcat/logs/catalina.out | wc -l
```

---

## セキュリティ操作

### SSH キーペア管理

```bash
# SSH 公開鍵追加
echo "ssh-rsa AAAA..." >> ~/.ssh/authorized_keys

# SSH 公開鍵削除
sed -i '/key_to_remove/d' ~/.ssh/authorized_keys

# ホスト鍵フィンガープリント確認
ssh-keygen -l -f /etc/ssh/ssh_host_rsa_key.pub
```

### ファイアウォール管理

```bash
# インバウンドルール確認
sudo firewall-cmd --list-all

# 外部公開はWireGuardのみ
sudo firewall-cmd --permanent --add-port=51820/udp
sudo firewall-cmd --permanent --remove-service=http
sudo firewall-cmd --permanent --remove-service=https
sudo firewall-cmd --permanent --remove-port=8080/tcp
sudo firewall-cmd --permanent --remove-port=3306/tcp

# 設定リロード
sudo firewall-cmd --reload
```

### SSL/TLS 証明書管理

```bash
# 証明書有効期限確認
openssl x509 -in /opt/certs/server.crt -noout -dates

# 証明書情報確認
openssl x509 -in /opt/certs/server.crt -noout -text

# Let's Encrypt 自動更新確認
sudo systemctl list-timers certbot
```

---

## ユーザー・権限管理

### MySQL ユーザー追加

```bash
# MySQL に接続
mysql -u root -p

# ユーザー追加
CREATE USER 'newuser'@'localhost' IDENTIFIED BY 'password';
GRANT SELECT ON taxtax.* TO 'newuser'@'localhost';
FLUSH PRIVILEGES;
```

### システムユーザー権限確認

```bash
# Tomcat ユーザー確認
id tomcat
groups tomcat

# MySQL ユーザー確認
id mysql
groups mysql

# EBS ボリュームアクセス権
ls -la /data/mysql
```

---

## ログ管理

### ログファイル確認

```bash
# アプリケーションログ
tail -f /opt/tomcat/logs/application.log
tail -f /opt/tomcat/logs/catalina.out

# MySQL ログ
tail -f /var/log/mysql/error.log
tail -f /var/log/mysql/slow-query.log  # スロークエリ

# デプロイログ
tail -f /var/log/deploy.log

# バックアップログ
tail -f /var/log/mysql-backup.log
```

### ログローテーション確認

```bash
# Tomcat ログローテーション
cat /opt/tomcat/bin/setenv.sh | grep logging

# logrotate 設定確認
cat /etc/logrotate.d/tomcat
cat /etc/logrotate.d/mysql-server
```

### ログアーカイブ

```bash
# 古いログをアーカイブ
gzip /opt/tomcat/logs/catalina.*.log

# アーカイブ一覧
ls -lh /opt/tomcat/logs/*.gz

# アーカイブ削除（90日以上）
find /opt/tomcat/logs -name "*.gz" -mtime +90 -delete
```

---

## Git・デプロイ管理

### リポジトリ状態確認

```bash
cd /home/ec2-user/tax-return

# ブランチ確認
git status

# コミットログ確認
git log --oneline -10

# リモート確認
git remote -v
```

### 手動デプロイ

```bash
# 最新コードをプルして再ビルド・デプロイ
sudo /opt/deploy.sh
```

### リバート（前バージョンに戻す）

```bash
# リポジトリから前のコミットにリセット
cd /home/ec2-user/tax-return
git reset --hard HEAD~1  # 直前のコミット
git push --force origin main

# デプロイ実行
sudo /opt/deploy.sh
```

---

## 定期メンテナンススケジュール

| 頻度 | 操作 | 実行時間 |
|---|---|---|
| **毎日 03:00** | MySQL バックアップ | cron |
| **毎週日曜** | ログローテーション | cron |
| **毎月 1 日** | ディスク容量確認 | 手動 |
| **3ヶ月ごと** | バックアップ復旧テスト | 手動 |
| **6ヶ月ごと** | セキュリティアップデート | 手動 |
| **12ヶ月ごと** | 災害復旧訓練 | 手動 |

---

## トラブルシューティング

### アプリケーションが起動しない

```bash
# 1. ステータス確認
sudo systemctl status tomcat

# 2. ログ確認
tail -f /opt/tomcat/logs/catalina.out

# 3. ポート確認
netstat -tlnp | grep 8080

# 4. 再起動試行
sudo systemctl restart tomcat
sleep 10
curl http://localhost:8080/final-tax-return-1.0-SNAPSHOT/home
```

### MySQL 接続エラー

```bash
# 1. MySQL ステータス確認
sudo systemctl status mysqld

# 2. 接続テスト
mysql -u taxapp -p -h localhost -e "SELECT 1"

# 3. ソケットファイル確認
ls -la /var/run/mysqld/mysqld.sock

# 4. 再起動
sudo systemctl restart mysqld
```

### ディスク容量満杯

```bash
# 1. 容量確認
df -h

# 2. 大きいファイル特定
du -sh /* | sort -rh | head -10

# 3. 古いバックアップ削除
find /data/mysql/backups -name "*.sql.gz" -mtime +7 -delete

# 4. ログ削除
find /opt/tomcat/logs -name "*.log" -mtime +30 -delete
```

---

## 災害復旧（DR）計画

### RTO/RPO 目標

| 項目 | RTO | RPO |
|---|---|---|
| アプリケーション | 1時間 | 15分 |
| データベース | 1時間 | 1日 |

### フェイルオーバー手順

```bash
# 1. バックアップ確認
ls -lh /data/mysql/backups/

# 2. 復旧用インスタンス起動
# AWS コンソールから新 EC2 インスタンス作成

# 3. EBS ボリュームアタッチ・マウント
sudo mount /dev/nvme1n1 /data/mysql

# 4. setup-ec2.sh 実行
bash /home/ec2-user/tax-return/scripts/setup-ec2.sh

# 5. バックアップから復旧
sudo /opt/restore-mysql.sh

# 6. アプリケーション起動
sudo systemctl start tomcat

# 7. ヘルスチェック
curl http://localhost:8080/final-tax-return-1.0-SNAPSHOT/home
```

---

**最終更新**: 2026-10-11
**連絡先**: [Team Email]
