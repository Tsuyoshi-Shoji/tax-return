# AWS Infrastructure Deployment Checklist

EC2 + MySQL + EBS 環境のデプロイメント完了チェックリスト

---

## Phase 1: AWS インフラ構築

### 1.1 EC2 インスタンス

- [ ] インスタンスを起動
  - [ ] AMI: Amazon Linux 2 または Ubuntu 22.04
  - [ ] Instance Type: t3.medium (2vCPU, 4GB RAM) 以上
  - [ ] Root Volume: 30GB (gp3)
  - [ ] Public IP: 割り当て

- [ ] セキュリティグループ設定
  - [ ] インバウンド: 22/tcp (SSH) - 制限対象 IP から
  - [ ] インバウンド: 51820/udp (WireGuard) - モバイル回線の変更を許容
  - [ ] TCP 80・443・8080・3306をIPv4・IPv6とも公開しない
  - [ ] [私用端末限定アクセス](PRIVATE-ACCESS.md) を設定・検証
  - [ ] アウトバウンド: すべて許可

- [ ] キーペア取得
  - [ ] .pem ファイルをローカル保管
  - [ ] パーミッション: 600 (`chmod 600`)
  - [ ] バックアップ保管

### 1.2 EBS ボリューム

- [ ] ボリューム作成
  - [ ] Size: 100GB (初期; 後で拡張可能)
  - [ ] Type: gp3 (General Purpose SSD)
  - [ ] Availability Zone: EC2 インスタンスと同じ
  - [ ] 暗号化: 有効（推奨）

- [ ] ボリュームをアタッチ
  - [ ] EC2 インスタンスに接続
  - [ ] Device: /dev/sdf

- [ ] EC2 内でマウント
  - [ ] Device 名確認: `lsblk`
  - [ ] ファイルシステム作成: `mkfs -t ext4`
  - [ ] マウントポイント作成: `/data/mysql`
  - [ ] マウント実行: `mount`
  - [ ] /etc/fstab に登録（再起動後も自動マウント）

---

## Phase 2: EC2 環境セットアップ

### 2.1 EC2 に接続

```bash
ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>
# または
ssh -i your-key.pem ubuntu@<EC2_PUBLIC_IP>
```

- [ ] SSH 接続確認

### 2.2 セットアップスクリプト実行

```bash
cd /tmp
git clone https://github.com/Tsuyoshi-Shoji/tax-return.git
cd tax-return
bash scripts/setup-ec2.sh
```

- [ ] スクリプト実行完了
- [ ] エラーログ確認: `tail /var/log/setup-ec2.log`

### 2.3 環境検証

```bash
# Java インストール確認
java -version

# Tomcat 起動確認
sudo systemctl status tomcat

# MySQL 起動確認
sudo systemctl status mysqld

# ポート確認
netstat -tlnp | grep -E '(java|mysql)'
```

- [ ] Java 17 インストール済み
- [ ] Tomcat 11 起動中
- [ ] MySQL 起動中
- [ ] Tomcat: ポート 8080 リスン
- [ ] MySQL: ポート 3306 リスン

### 2.4 認証情報確認

```bash
sudo cat /opt/db-credentials.env
```

- [ ] DB_NAME: taxtax
- [ ] DB_USER: taxapp
- [ ] DB_PASS: (ランダムに生成)

---

## Phase 3: GitHub Actions 設定

### 3.1 GitHub Secrets 登録

リポジトリの Settings → Secrets に登録:

```
EC2_HOST=<EC2 パブリック IP / IPv6 / ホスト名>
EC2_PRIVATE_KEY=<キーペアの内容（-----BEGIN から -----END まで）>
```

- [ ] `EC2_HOST` 登録
- [ ] `EC2_PRIVATE_KEY` 登録

### 3.2 ワークフロー確認

```bash
cat .github/workflows/deploy-ec2.yml
```

- [ ] `.github/workflows/deploy-ec2.yml` ファイルが存在
- [ ] `EC2_HOST` と `EC2_PRIVATE_KEY` を参照している

### 3.3 テスト実行

```bash
# main ブランチにコミット・プッシュ
git add .
git commit -m "Initial EC2 deployment"
git push origin main
```

- [ ] GitHub Actions ワークフロー実行開始
- [ ] Actions タブで実行状況確認

### 3.4 デプロイメント検証

```bash
# GitHub Actions ログ確認
# 以下の手順が完了していることを確認:
# 1. Build and Test (Maven build)
# 2. Deploy via SSH (SSH接続 → deploy.sh実行)
# 3. Health Check (HTTP 200応答)
```

- [ ] Maven Build: ✓ PASS
- [ ] SSH Deploy: ✓ PASS
- [ ] Health Check: ✓ PASS

---

## Phase 4: デプロイメント検証

### 4.1 ローカルからのアクセステスト

```bash
# IPv6 の場合は curl http://[<EC2_PUBLIC_IP>]:8080/final-tax-return-1.0-SNAPSHOT/home
curl http://<EC2_PUBLIC_IP>:8080/final-tax-return-1.0-SNAPSHOT/home
```

- [ ] HTTP 200 応答
- [ ] HTML コンテンツ確認

### 4.2 EC2 内部の検証

```bash
# SSH ログイン
ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>

# Tomcat ログ確認
tail -f /opt/tomcat/logs/catalina.out

# デプロイログ確認
tail -f /var/log/deploy.log

# MySQL 接続テスト
mysql -u taxapp -p -h localhost -e "SELECT 1"
```

- [ ] Tomcat ログにエラーなし
- [ ] MySQL 接続成功
- [ ] アプリケーションが起動中

### 4.3 ダイナミックコンテンツ検証

以下の URL にアクセス:
- [ ] `/home` - ホームページ表示
- [ ] `/tax-return` - 税務データページ表示
- [ ] `/api/*` - API エンドポイント機能確認

---

## Phase 5: バックアップ・復旧設定

### 5.1 バックアップ確認

```bash
# バックアップスクリプト実行
sudo /opt/backup-mysql.sh

# バックアップファイル確認
ls -lh /data/mysql/backups/
```

- [ ] バックアップファイル生成
- [ ] 圧縮ファイル（.sql.gz）確認

### 5.2 バックアップスケジュール確認

```bash
sudo crontab -l | grep backup
```

- [ ] cron ジョブが登録されている
- [ ] 実行時刻: 03:00 UTC（毎日）

### 5.3 復旧テスト（非本番環境推奨）

```bash
# テスト用 EC2 インスタンスで:
sudo /opt/restore-mysql.sh -d taxtax_test \
  -f /data/mysql/backups/backup_latest.sql.gz
```

- [ ] バックアップから復旧成功
- [ ] データ整合性確認

---

## Phase 6: セキュリティ設定

### 6.1 SSH キー保護

- [ ] ローカルマシンで .pem ファイルのバックアップ
- [ ] パーミッション確認: `chmod 600 ~/.ssh/*.pem`
- [ ] 安全な場所に保管

### 6.2 ファイアウォール確認

```bash
sudo firewall-cmd --list-all
```

- [ ] SSH (22): 制限対象 IP から許可
- [ ] Tomcat (8080): 必要な送信元から許可
- [ ] MySQL (3306): 内部のみ（外部からは拒否）

### 6.3 MySQL セキュリティ

```bash
# MySQL root パスワード確認
sudo mysql -u root -p  # パスワードなしで接続不可か確認

# ユーザー権限確認
mysql -u taxapp -p -h localhost -e "SHOW GRANTS;"
```

- [ ] root ユーザーがパスワード保護
- [ ] root リモートログイン無効
- [ ] taxapp ユーザーは localhost のみ

### 6.4 Tomcat セキュリティ

```bash
# Tomcat ユーザー確認
ls -la /opt/tomcat

# デフォルトアプリ削除
sudo rm -rf /opt/tomcat/webapps/ROOT
sudo rm -rf /opt/tomcat/webapps/manager
sudo rm -rf /opt/tomcat/webapps/host-manager
```

- [ ] Tomcat ユーザーは tomcat（root 以外）
- [ ] デフォルトアプリ削除（不要なもの）

---

## Phase 7: 監視・ロギング設定

### 7.1 ログローテーション

```bash
# Tomcat ログローテーション確認
cat /opt/tomcat/bin/setenv.sh | grep logging

# MySQL スロークエリログ確認
mysql -u root -p -e "SHOW VARIABLES LIKE 'slow_query_log%';"
```

- [ ] Tomcat ログローテーション設定済み
- [ ] MySQL スロークエリログ有効

### 7.2 ヘルスチェック

```bash
# ヘルスチェック手動実行
sudo /opt/health-check.sh

# ヘルスチェックログ確認
tail -f /var/log/health-check.log
```

- [ ] ヘルスチェック成功
- [ ] すべてのコンポーネント UP

### 7.3 CloudWatch 設定（オプション）

- [ ] CloudWatch Logs Agent インストール
- [ ] Tomcat ログを CloudWatch に送信
- [ ] MySQL ログを CloudWatch に送信
- [ ] アラーム設定: CPU > 80%, Disk > 80%

---

## Phase 8: 運用手順確認

### 8.1 ドキュメント確認

- [ ] `docs/EC2-DEPLOYMENT.md` - デプロイメント手順
- [ ] `docs/GITHUB-SECRETS.md` - GitHub Secrets 設定
- [ ] `docs/OPERATIONS-RUNBOOK.md` - 本番運用手順

### 8.2 스크립트 확인

- [ ] `/opt/deploy.sh` - デプロイスクリプト
- [ ] `/opt/backup-mysql.sh` - バックアップスクリプト
- [ ] `/opt/restore-mysql.sh` - 復旧スクリプト
- [ ] `/opt/health-check.sh` - ヘルスチェックスクリプト

### 8.3 운영 체크리스트

- [ ] 일일 체크리스트 작성
- [ ] 주간 체크리스트 작성
- [ ] 월간 체크리스트 작성

---

## Phase 9: 재해 복구（DR）계획

### 9.1 RTO/RPO 설정

- [ ] RTO (Recovery Time Objective) 설정: 1시간
- [ ] RPO (Recovery Point Objective) 설정: 1일

### 9.2 재해 복구 문서

- [ ] 페일오버 절차 작성
- [ ] 백업 복구 절차 작성
- [ ] 통신 절차 작성

### 9.3 재해 복구 훈련

- [ ] 분기별 DR 훈련 계획
- [ ] 결과 기록 및 개선사항 추출

---

## Phase 10: 최종 체크

### 10.1 기능 테스트

- [ ] 홈페이지 접속 확인
- [ ] 세금 환급 데이터 저장/조회 확인
- [ ] 보고서 생성 확인
- [ ] 로그아웃 확인

### 10.2 성능 검증

```bash
# 메모리 사용 확인
free -h

# 디스크 사용 확인
df -h /data/mysql

# 네트워크 응답 확인
time curl http://localhost:8080/final-tax-return-1.0-SNAPSHOT/home
```

- [ ] 메모리 사용: < 70%
- [ ] 디스크 사용: < 60%
- [ ] 응답 시간: < 2초

### 10.3 문서화

- [ ] 구성 다이어그램 작성
- [ ] 운영 절차서 작성
- [ ] 문제 해결 가이드 작성

---

## 완료 서명

| 역할 | 이름 | 서명 | 날짜 |
|---|---|---|---|
| 배포 담당자 | | | |
| 운영 담당자 | | | |
| 보안 담당자 | | | |

---

## 추가 자료

- [AWS EC2 설명서](https://docs.aws.amazon.com/ec2/)
- [Tomcat 문서](https://tomcat.apache.org/)
- [MySQL 문서](https://dev.mysql.com/doc/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)

**마지막 업데이트**: 2026-10-11
