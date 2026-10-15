# Scripts Directory - EC2 Deployment Support

EC2 上での環境構築・デプロイメント・運用に使用するスクリプト一覧です。

私用端末限定の接続は [WireGuard + HTTPS手順](../docs/PRIVATE-ACCESS.md) を使用します。初期セットアップはTomcatをloopbackに限定しますが、VPN・証明書・NginxとAWSの公開範囲は別途設定が必要です。既存ホストへの初期セットアップ再実行はしないでください。

---

## 📋 スクリプト一覧

### 1. **setup-ec2.sh** - EC2 環境初期構築
- **用途**: EC2 インスタンスを本番環境として構成
- **実行ユーザー**: root (sudo 必須)
- **実行タイミング**: インスタンス初回起動時（1回のみ）
- **実行時間**: 約 5-10 分
- **前提条件**:
  - EC2 インスタンスが起動中
  - EBS ボリュームがマウント可能（/dev/nvme1n1）
  - インターネット接続確認

#### 実行方法
```bash
# リポジトリをクローン
git clone https://github.com/Tsuyoshi-Shoji/tax-return.git
cd tax-return

# スクリプト実行
bash scripts/setup-ec2.sh
```

#### 実行内容
- Java 21 + Tomcat 11 インストール
- MySQL インストール・初期化
- EBS ボリュームマウント・設定
- MySQL データベース・ユーザー作成
- Tomcat 環境設定
- Tomcatのloopback限定待受とCookie保護設定
- デプロイスクリプト配置
- バックアップスクリプト設定
- cron ジョブ登録
- ファイアウォール設定

DBパスワードはJVM起動引数ではなく `DB_PASS` 環境変数で渡します。プロセス環境を読める管理者・同一ユーザーからは保護できないため、Tomcatの実行ユーザーを専用にします。

#### ログ確認
```bash
tail -f /var/log/setup-ec2.log
```

---

### 2. **deploy.sh** - アプリケーション自動デプロイメント
- **用途**: GitHub Actions から呼び出され、アプリをビルド・デプロイ
- **実行ユーザー**: root (sudo 必須)
- **実行タイミング**: GitHub Actions ワークフロー内（main ブッシュ時）
- **実行時間**: 約 2-5 分
- **前提条件**:
  - Java 21・Maven インストール済み
  - systemd の `tomcat.service` が設定済みで起動可能な状態
  - MySQL 起動中

#### 実行方法
```bash
# 手動実行（テスト用）
sudo /opt/deploy.sh

# GitHub Actions から自動実行（SSH経由）
ssh -i key.pem ec2-user@HOST 'sudo /opt/deploy.sh'
```

#### 実行内容
- リポジトリ pull
- Maven build（テストスキップ）
- systemd 経由で Tomcat 停止（残存プロセスがあればデプロイ中断）
- 古い WAR ファイル削除
- 新しい WAR ファイルデプロイ
- systemd 経由で Tomcat 起動・サービスの稼働確認
- Flyway のバージョン管理済み DB マイグレーション適用後、Hibernate でスキーマ検証
- アプリケーションのログイン画面をヘルスチェック（最大 30 回、2 秒間隔）

DB マイグレーションは `src/main/resources/db/migration` にバージョン順で追加します。既存データベースを破壊する変更を追加せず、起動時に Hibernate の自動更新も行いません。

起動・停止は `systemctl` に統一し、`catalina.sh start/stop` を直接併用しないでください。
`Type=simple` / `catalina.sh run` のサービスはPIDファイルを更新しないため、古いPIDファイルを使った直接停止は失敗することがあります。
サービス操作のエラーはデプロイログと GitHub Actions の両方に出力されます。

#### 既存ホストへのスクリプト更新

リポジトリ側の変更だけでは、配置済みの `/opt/deploy.sh` は更新されません。
修正のコミット・push後、ホストのリポジトリを更新し、内容を確認してから配置してください。
デプロイは未コミット変更があると中断します。

```bash
bash -n scripts/deploy.sh
sudo cp -p /opt/deploy.sh "/opt/deploy.sh.$(date +%Y%m%d-%H%M%S).bak"
sudo install -o root -g root -m 0755 scripts/deploy.sh /opt/deploy.sh
sudo cmp scripts/deploy.sh /opt/deploy.sh
```

この配置操作ではサービスを再起動しません。サービス定義やPIDファイルの変更も不要です。
デプロイ時の停止・起動は既存の `tomcat.service` の設定に従います。

#### ログ確認
```bash
tail -f /var/log/deploy.log
sudo journalctl -u tomcat.service -n 100 --no-pager
```

#### トラブルシューティング
```bash
# Maven ビルドエラーの場合
cd /home/ec2-user/tax-return
mvn clean package -DskipTests

# Tomcat 起動エラーの場合
sudo systemctl restart tomcat
sudo systemctl status tomcat

# アプリケーションのヘルスチェック
curl -i http://localhost:8080/final-tax-return-1.0-SNAPSHOT/login
```

#### 回帰テスト（サービスの停止・起動は行わない）
```bash
python3 -B -m unittest discover -s scripts/ci -p 'test_deploy.py' -v
```

---

### 3. **backup-mysql.sh** - MySQL 自動バックアップ
- **用途**: MySQL データベースのバックアップ作成
- **実行ユーザー**: root
- **実行タイミング**: 毎日 03:00 UTC（cron 自動実行）、または手動
- **実行時間**: 約 30 秒～2 分（データベースサイズに依存）
- **前提条件**:
  - MySQL 起動中
  - `/data/mysql/backups/` ディレクトリ作成済み
  - ディスク容量十分（バックアップサイズ分）

#### 実行方法
```bash
# 手動バックアップ実行
sudo /opt/backup-mysql.sh

# 複数回実行（cron から自動）
sudo crontab -l | grep backup
```

#### 実行内容
- MySQL `taxtax` データベースをダンプ
- gzip で圧縮
- `/data/mysql/backups/` に保存
- バックアップ完全性チェック（gzip -t）
- 7 日以上前のバックアップを自動削除
- バックアップ一覧をログに出力

#### ログ確認
```bash
tail -f /var/log/mysql-backup.log
```

#### バックアップファイル確認
```bash
# バックアップ一覧
ls -lh /data/mysql/backups/

# バックアップサイズ
du -sh /data/mysql/backups/

# 直近のバックアップ確認
ls -lth /data/mysql/backups/ | head -5
```

#### トラブルシューティング
```bash
# MySQL 接続エラーの場合
mysql -u taxapp -p -h localhost -e "SELECT 1"

# 認証情報確認
sudo cat /opt/db-credentials.env

# バックアップファイルが生成されない場合
sudo /opt/backup-mysql.sh -v
```

---

### 4. **restore-mysql.sh** - MySQL バックアップから復旧
- **用途**: バックアップファイルから MySQL を復旧
- **実行ユーザー**: root
- **実行タイミング**: 手動（復旧が必要な場合）
- **実行時間**: 約 1-5 分（バックアップサイズに依存）
- **前提条件**:
  - MySQL 起動中
  - バックアップファイル存在
  - ディスク容量十分

#### 実行方法
```bash
# 最新バックアップから復旧
sudo /opt/restore-mysql.sh

# 特定のバックアップから復旧
sudo /opt/restore-mysql.sh -f /data/mysql/backups/backup_20261011_030000.sql.gz

# 別のデータベースへ復旧（テスト用）
sudo /opt/restore-mysql.sh -d taxtax_test
```

#### コマンドオプション
```bash
-f FILE       # 復旧するバックアップファイル（デフォルト: 最新）
-d DATABASE   # 復旧先データベース名（デフォルト: taxtax）
-h            # ヘルプ表示
```

#### 実行内容
- バックアップファイル完全性チェック
- 既存データベースをバックアップ（復旧失敗時の復元用）
- データベース再作成
- バックアップから復旧
- テーブル数確認
- ログ出力

#### ログ確認
```bash
tail -f /var/log/mysql-restore.log
```

#### トラブルシューティング
```bash
# バックアップファイルが見つからない場合
ls -lh /data/mysql/backups/

# 復旧前のバックアップ確認
ls -lh /data/mysql/backups/pre-restore_*

# バックアップ完全性確認
gzip -t /data/mysql/backups/backup_latest.sql.gz
```

---

### 5. **health-check.sh** - ヘルスチェック
- **用途**: アプリケーション・MySQL・Tomcat の稼働確認
- **実行ユーザー**: root（または一般ユーザー）
- **実行タイミング**: 定期実行（cron）、または監視ツールから
- **実行時間**: 約 10 秒
- **前提条件**:
  - Tomcat・MySQL 起動中
  - curl コマンド利用可能

#### 実行方法
```bash
# 手動実行
/opt/health-check.sh

# cron 登録（定期実行）
*/5 * * * * /opt/health-check.sh  # 5分ごと
```

#### 実行内容
- HTTP ヘルスチェック（/home エンドポイント）
- MySQL 接続確認
- Tomcat プロセス確認
- 総合ステータス判定

#### ログ確認
```bash
tail -f /var/log/health-check.log
```

#### 返り値
```bash
# 成功時
exit 0  # すべて UP

# 失敗時
exit 1  # 何らか DOWN
```

#### 監視ツール連携例
```bash
# Nagios/Icinga
check_command check_by_ssh -H ec2-host -C '/opt/health-check.sh'

# Zabbix
UserParameter=app.health,/opt/health-check.sh && echo 1 || echo 0
```

---

### 6. **tomcat-setenv.sh** - Tomcat 環境変数設定
- **用途**: Tomcat 起動時の Java オプション・環境変数設定
- **配置先**: `/opt/tomcat/bin/setenv.sh`
- **読み込み**: Tomcat 起動時に自動読み込み
- **前提条件**:
  - `/opt/db-credentials.env` 存在（自動読み込み）

#### 設定内容
```bash
# Java メモリ
-Xms1024m -Xmx2048m  # ヒープサイズ

# GC 設定
-XX:+UseG1GC  # G1 ガベージコレクタ

# その他
-Dfile.encoding=UTF-8  # ファイルエンコーディング
-Duser.timezone=UTC    # タイムゾーン
```

#### カスタマイズ
インスタンスサイズに応じてメモリ調整:

| インスタンス | 設定値 |
|---|---|
| t3.medium (4GB) | Xms=1024m, Xmx=2048m |
| t3.large (8GB) | Xms=2048m, Xmx=4096m |
| t3.xlarge (16GB) | Xms=4096m, Xmx=8192m |

---

### 7. **tomcat.service** - Systemd サービスファイル
- **用途**: Tomcat を systemd で管理
- **配置先**: `/etc/systemd/system/tomcat.service`
- **使用方法**:
  ```bash
  sudo systemctl start tomcat     # 起動
  sudo systemctl stop tomcat      # 停止
  sudo systemctl restart tomcat   # 再起動
  sudo systemctl enable tomcat    # 自動起動有効
  ```

---

## 📝 実行権限・所有者

```bash
# スクリプトファイルの権限設定
chmod 755 /opt/deploy.sh
chmod 755 /opt/backup-mysql.sh
chmod 755 /opt/restore-mysql.sh
chmod 755 /opt/health-check.sh

# 所有者
chown root:root /opt/*.sh
```

---

## 🔐 セキュリティノート

1. **認証情報**: `/opt/db-credentials.env` は `600` パーミッションで保護
2. **スクリプト実行**: root または sudo 必須（機密データへのアクセス）
3. **ログ出力**: パスワード・秘密情報はログに出力されない
4. **バックアップ**: `/data/mysql/backups/` は `700` パーミッションで保護

---

## 🐛 トラブルシューティング

### スクリプト実行エラー

```bash
# 1. スクリプト権限確認
ls -la /opt/*.sh

# 2. インタプリタ確認
head -1 /opt/deploy.sh  # #!/bin/bash

# 3. ログ確認
tail -f /var/log/*.log

# 4. 手動実行でデバッグ
bash -x /opt/deploy.sh  # トレース実行
```

### cron ジョブが実行されない

```bash
# cron ジョブ確認
sudo crontab -l

# cron ログ確認（CentOS/RHEL）
grep CRON /var/log/cron

# cron ログ確認（Ubuntu）
grep CRON /var/log/syslog
```

### バックアップ失敗

```bash
# ディスク容量確認
df -h /data/mysql

# MySQL プロセス確認
ps aux | grep mysqld

# MySQL ログ確認
sudo tail -f /var/log/mysql/error.log
```

---

## 📚 関連ドキュメント

- `EC2-DEPLOYMENT.md` - デプロイメント手順
- `GITHUB-SECRETS.md` - GitHub Secrets 設定
- `OPERATIONS-RUNBOOK.md` - 本番運用ガイド
- `AWS-DEPLOYMENT-CHECKLIST.md` - 完了チェックリスト

---

**最終更新**: 2026-10-11
