# GitHub Secrets 設定手順

GitHub Actions から EC2 へ SSH デプロイする場合に必要な認証情報を Secrets に登録します。

## Runner と接続経路の前提

`ubuntu-latest` から IPv6-only の EC2 への到達性を前提にしないでください。実行 `38569824233` では WAR のビルドは成功しましたが、IPv6 の `ssh-keyscan` が終了コード 1 を返し、標準エラーが破棄されていました。

- IPv4 を使用する場合は、runner から到達可能な IPv4 / ホスト名を `EC2_HOST` に登録します。
- IPv6-only または VPN 内の管理アドレスを使用する場合は、その接続先へ SSH 接続できる専用 Linux runner を用意します。**Settings → Actions → Runners** から登録し、`ec2-deploy` ラベルを付けます。**Settings → Secrets and variables → Actions → Variables** に `EC2_DEPLOY_RUNNER` を JSON 配列 `["self-hosted","linux","ec2-deploy"]` として登録します。未設定時は `"ubuntu-latest"` を使用します。
- runner には Java 21、Maven、Git、OpenSSH、curl、iproute2 が必要です。IPv6 の経路、EC2 側の SSH 待受、セキュリティグループ・ホストファイアウォールの接続元制限を確認してください。runner の登録だけでは IPv6 / VPN 接続は提供されません。
- runner は本番アプリや DB のホストとは分離し、信頼する `main` のデプロイ専用にアクセス制限します。外部 PR のコードを実行させず、可能ならジョブごとに破棄する構成を使用します。
- TCP 80・443・8080・3306 を公開する必要はありません。デプロイ後の確認は SSH 経由で EC2 の `http://127.0.0.1:8080/final-tax-return-1.0-SNAPSHOT/login` に対して行います。利用者の接続先は [VPN 内 HTTPS](PRIVATE-ACCESS.md) です。

この設定と以下の Secrets 登録は管理者が別途行う必要があります。変更を戻す場合はワークフローを戻し、専用 runner の登録・権限を解除します。復旧のためにアプリの公開ポートを開けないでください。

## 設定手順

### 1. EC2 キーペア情報取得

#### Windows の場合
```powershell
# .pem ファイルをテキストエディタで開く
notepad C:\Users\<username>\.ssh\your-key-pair.pem

# または PowerShell で
Get-Content C:\Users\<username>\.ssh\your-key-pair.pem
```

#### macOS/Linux の場合
```bash
cat ~/.ssh/your-key-pair.pem
```

### 2. GitHub リポジトリ設定

#### 手順 A: GitHub Web UI から登録

1. リポジトリトップページで **Settings** をクリック
2. 左メニューから **Secrets and variables** → **Actions** をクリック
3. **New repository secret** をクリック

#### 手順 B: gh CLI を使用

```bash
# GitHub CLI インストール（未済の場合）
# https://cli.github.com/

# ログイン
gh auth login

# Secrets 登録
gh secret set EC2_HOST --body "your-ec2-public-ip-or-hostname"
gh secret set EC2_PRIVATE_KEY < ~/.ssh/your-key-pair.pem
```

### 3. Secrets 登録内容

#### Secret 1: EC2_HOST

| 項目 | 値 |
|---|---|
| **Name** | `EC2_HOST` |
| **Secret** | EC2 インスタンスのパブリック IP / IPv6 / ホスト名 |

**例:**
```
203.0.113.45
または
2406:da14:117f:7b2c:dd13:5665:e938:7d
または
ec2-203-0-113-45.ap-northeast-1.compute.amazonaws.com
```

**取得方法:**
```bash
# EC2 ダッシュボードから確認、またはコマンドで取得
aws ec2 describe-instances --instance-ids i-xxxxxxxxx \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text
```

#### Secret 2: EC2_PRIVATE_KEY

| 項目 | 値 |
|---|---|
| **Name** | `EC2_PRIVATE_KEY` |
| **Secret** | EC2 キーペア (.pem ファイルの全内容) |

**例:**
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA2xk7z8+...
...
-----END RSA PRIVATE KEY-----
```

**重要:** 
- ファイルの最初の `-----BEGIN RSA PRIVATE KEY-----` から最後の `-----END RSA PRIVATE KEY-----` まで全て をコピー
- 改行を含める
- テキストの前後に余分なスペースを含めない

#### Secret 3: EC2_KNOWN_HOSTS

EC2 の SSH ホスト公開鍵を、`EC2_HOST` と同じ接続先表記の `known_hosts` エントリーとして登録します（IPv6 は角括弧なし）。管理済みの EC2 コンソール等で `/etc/ssh/ssh_host_ed25519_key.pub` とそのフィンガープリントを確認し、信頼済みの値を使用してください。接続時の未検証の `ssh-keyscan` 結果をそのまま信頼しないでください。

```text
<EC2_HOST> ssh-ed25519 <検証済みのホスト公開鍵>
```

検証済みエントリーを管理用端末の Git 管理外のファイルに保存して登録します：

```bash
gh secret set EC2_KNOWN_HOSTS < ~/.ssh/ec2-deploy-known_hosts
```

ホスト鍵の変更時は管理経路から正当性を再確認して更新します。`StrictHostKeyChecking` を無効化しないでください。

### 4. Secrets 検証

ワークフローの `Deploy and verify via SSH` は、ホスト鍵検証を有効にして `sudo -n /opt/deploy.sh` を実行します。EC2 側でこの管理済みスクリプトに限定したパスワード不要の sudo 許可が必要です。DB 認証情報は EC2 側のスクリプトで読み込むため、SSH ユーザーによる `/opt/db-credentials.env` の読み込みや GitHub へのコピーは不要です。SSH 鍵と設定はジョブの一時ディレクトリに作成され、終了時に削除されます。

### 5. キーペアのセキュリティ

#### ✅ ベストプラクティス

```bash
# キーペアのアクセス権限を 600 に設定
chmod 600 ~/.ssh/your-key-pair.pem

# GitHub Secrets に保管（平文でコード保存しない）
# リポジトリに .gitignore で除外
echo "*.pem" >> .gitignore
```

#### ⚠️ 危険な操作

```bash
# 以下はしないこと
# × ソースコードに直接記載
# × 環境変数ファイルをコミット
# × Slack/メールで共有
# × Git history に含める
```

### 6. キーペア喪失時の対応

EC2 キーペアを喪失した場合：

#### オプション 1: EC2 Instance Connect（推奨）

```bash
# AWS CLI で SSM Session Manager を使用
aws ssm start-session --target i-xxxxxxxxx
```

#### オプション 2: EC2 Instance Recovery

1. EC2 ダッシュボードから new キーペアを生成
2. 既存インスタンスから EBS ボリュームをデタッチ
3. 新しいインスタンスにアタッチ・初期化

#### オプション 3: AMI スナップショットから復旧

```bash
# 既存インスタンスから AMI を作成
# 新しいキーペアで新インスタンス起動
aws ec2 create-image --instance-id i-xxxxxxxxx --name "tax-return-backup"
```

### 7. Secrets のローテーション（推奨: 90日ごと）

```bash
# 新しいキーペア生成
# GitHub Secrets を更新
gh secret set EC2_PRIVATE_KEY < ~/.ssh/new-key-pair.pem

# EC2 上の ~/.ssh/authorized_keys を更新
ssh -i ~/.ssh/new-key-pair.pem ec2-user@<EC2_HOST>
# 公開鍵を authorized_keys に追加
```

### 8. 複数環境での管理

開発・ステージング・本番 で異なるキーペアを使用する場合：

```bash
# Secrets 命名規則
gh secret set DEV_EC2_HOST --body "dev-ec2-ip"
gh secret set DEV_EC2_PRIVATE_KEY < ~/.ssh/dev-key.pem

gh secret set PROD_EC2_HOST --body "prod-ec2-ip"
gh secret set PROD_EC2_PRIVATE_KEY < ~/.ssh/prod-key.pem
```

ワークフロー側で環境分岐：
```yaml
- uses: appleboy/ssh-action@master
  with:
    host: ${{ secrets[format('{0}_EC2_HOST', env.ENVIRONMENT)] }}
    key: ${{ secrets[format('{0}_EC2_PRIVATE_KEY', env.ENVIRONMENT)] }}
```

---

## トラブルシューティング

### Q: "Permission denied (publickey)"エラー

**原因:** キーペアが正しく登録されていない

**対応:**
```bash
# キーペア内容確認
cat ~/.ssh/your-key-pair.pem | wc -l  # 行数確認
cat ~/.ssh/your-key-pair.pem | head -1  # 最初の行が "-----BEGIN RSA PRIVATE KEY-----" か確認

# GitHub Secrets を再登録
gh secret delete EC2_PRIVATE_KEY
gh secret set EC2_PRIVATE_KEY < ~/.ssh/your-key-pair.pem
```

### Q: "Host key verification failed"エラー

**原因:** 接続先と `EC2_KNOWN_HOSTS` が一致しない、またはホスト鍵が変更された

**対応:**
```bash
# EC2 IP アドレス確認
aws ec2 describe-instances --instance-ids i-xxxxxxxxx \
  --query 'Reservations[0].Instances[0].PublicIpAddress'

# 管理済みの経路でホスト鍵と接続先を再確認して Secrets を更新
gh secret set EC2_KNOWN_HOSTS < ~/.ssh/ec2-deploy-known_hosts
```

### Q: "Connection timed out"エラー

**原因:** セキュリティグループが SSH を許可していない

**対応:**
```bash
# EC2 セキュリティグループを確認
aws ec2 describe-security-groups --group-ids sg-xxxxxxxxx

# 専用 runner の接続元だけに SSH (22) を許可
# IPv6-only の場合は上記 EC2_DEPLOY_RUNNER 設定と IPv6 経路も確認
```

### Q: SSH キーペアのフォーマットエラー

**原因:** OpenSSH 形式と RSA 形式の混在

**対応:**
```bash
# RSA 形式に統一
ssh-keygen -p -f ~/.ssh/your-key-pair.pem -m pem -N ""

# または、OpenSSH 形式に変換
ssh-keygen -p -f ~/.ssh/your-key-pair.pem -m openssl -N ""
```

---

## 監査・ログ

GitHub Actions の実行ログでは Secrets の値は `***` でマスクされます：

```
with:
  host: 203.0.113.45
  key: ***  # 隠されている
```

詳細ログ有効時でも値は隠されるため安全です。

---

**最終更新**: 2026-10-11
