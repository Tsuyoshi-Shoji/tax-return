# 本番環境セキュリティガイド

## 秘密情報管理

### GitHub Secrets（推奨）

本番環境に必要な秘密情報は **GitHub Secrets** で管理します。

- **EC2_HOST**: EC2 インスタンス IP / IPv6 / ドメイン
- **EC2_PRIVATE_KEY**: SSH 秘密鍵

### EC2 環境変数管理

1. **自動生成ファイル**: `/opt/db-credentials.env`
   - `setup-ec2.sh` 実行時に自動生成
   - ランダムなパスワード設定（OpenSSL）
   - パーミッション: `600` (root のみ読み取り可能)

2. **セキュアな読み込み**:
   ```bash
   source /opt/db-credentials.env  # Tomcat setenv.sh で使用
   ```

3. **アクセス制限**:
   ```bash
   # ファイル権限確認
   ls -la /opt/db-credentials.env
   # -rw------- 1 root root  ...
   ```

## ログ管理とマスキング

### デプロイログ（/var/log/deploy.log）

- ✅ コミット SHA 記録
- ✅ ビルド結果
- ✅ ヘルスチェック結果
- ❌ パスワード
- ❌ API キー
- ❌ 秘密鍵

### アプリケーションログ

`application-production.properties` で設定：
```properties
logging.level.root=WARN
logging.level.org.example=INFO
server.error.include-message=never
server.error.include-stacktrace=never
```

## アクセス制御

### GitHub リポジトリ

- **Default branch**: `main`（保護ブランチ）
- **require status checks**: 継続的インテグレーション必須
- **require code review**: オプション（チーム規模による）
- **dismiss stale reviews**: オプション

### EC2 インスタンス

**ファイアウォール設定**:
```bash
# HTTP/HTTPS のみ許可
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --remove-port=3306/tcp
sudo firewall-cmd --reload
```

**SSH アクセス**:
- EC2 キーペア認証のみ（パスワード認証無効）
- 特定 IP からのアクセスに制限可能

### MySQL

**ユーザー権限**:
```sql
-- アプリケーション用ユーザー（権限最小化）
GRANT ALL PRIVILEGES ON taxtax.* TO 'taxapp'@'localhost';

-- root ユーザーはローカルのみ
CREATE USER 'root'@'localhost' IDENTIFIED BY '...';
```

## バックアップと復旧

### 自動バックアップ

**スケジュール**: 毎日 03:00（UTC）
```bash
crontab -l
# 0 3 * * * /opt/backup-mysql.sh
```

**保存先**: `/data/mysql/backups/`

**保有期間**: 7日間（自動削除）

### 手動バックアップ

```bash
source /opt/db-credentials.env
mysqldump -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" > backup-manual.sql
gzip backup-manual.sql
```

### リストア手順

```bash
source /opt/db-credentials.env
gunzip -c backup-2024-01-15-030000.sql.gz | \
  mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME"
```

## デプロイロールバック

### 前の版に戻す

```bash
# 利用可能なバックアップを確認
ls -la /opt/backups/war/

# 前の WAR をリストア
cp /opt/backups/war/final-tax-return-1.0-SNAPSHOT.war.20240115-150000.bak \
   /opt/tomcat/webapps/final-tax-return-1.0-SNAPSHOT.war

# Tomcat 再起動
sudo systemctl restart tomcat
```

### Git でロールバック

```bash
cd /home/ec2-user/tax-return
git log --oneline
git reset --hard <commit-hash>
/opt/deploy.sh
```

## 監視とアラート

### ログ監視

```bash
# デプロイログ
tail -f /var/log/deploy.log

# アプリケーションログ
tail -f /opt/tomcat/logs/catalina.out
tail -f /opt/tomcat/logs/application.log

# MySQL ログ
tail -f /var/log/mysql/error.log
tail -f /var/log/mysql/slow-query.log

# バックアップログ
tail -f /var/log/mysql-backup.log
```

### ヘルスチェック

デプロイワークフロー内で自動実行：
- HTTP エンドポイント: `/final-tax-return-1.0-SNAPSHOT/home`
- 期待値: HTTP 200
- リトライ: 30 回（60 秒）

## インシデント対応

### デプロイ失敗時

1. **ログ確認**
   ```bash
   tail -f /var/log/deploy.log
   tail -f /opt/tomcat/logs/catalina.out
   ```

2. **Tomcat 状態確認**
   ```bash
   systemctl status tomcat
   ps aux | grep java
   ```

3. **ロールバック実行**
   ```bash
   /opt/deploy.sh  # 前のバージョンが残っていれば復旧
   ```

### MySQL 問題時

```bash
# MySQL サービス確認
systemctl status mysqld

# 接続テスト
source /opt/db-credentials.env
mysql -u "$DB_USER" -p"$DB_PASS" -h localhost "$DB_NAME" -e "SELECT 1;"
```

## 定期メンテナンス

### 週 1 回
- デプロイログ確認
- MySQL ログ確認（エラー、スロークエリ）

### 月 1 回
- バックアップ復旧テスト（本番以外で実施）
- セキュリティ設定レビュー

### 年 1 回
- サーバー性能ベンチマーク
- Java/MySQL バージョン更新検討
- セキュリティ監査
