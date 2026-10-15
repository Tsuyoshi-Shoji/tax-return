# GitHub Secrets 設定手順

GitHub Actions から EC2 へ SSH デプロイする場合に必要な認証情報を Secrets に登録します。

現在の一時SSH許可方式の設定は、[OIDC・専用SG・Variables/Secretsの手順](GITHUB-ACTIONS-SSH.md)
を優先してください。`EC2_HOST`はパブリックIPv4を使用し、`EC2_KNOWN_HOSTS`も必須です。
AWSの長期アクセスキーは使用しません。

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
| **Secret** | EC2 インスタンスのパブリックIPv4（現在の一時許可方式） |

**例:**
```
203.0.113.45
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

### 4. Secrets 検証

`EC2_KNOWN_HOSTS`とOIDC用Variablesも登録したうえでpushします。
Actionsの「Validate deployment settings」「Configure short-lived AWS credentials」
「Temporarily allow runner SSH」「Revoke this run's SSH permission」を確認してください。

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

**原因:** EC2_HOST が正しくない、またはホスト名に誤字

**対応:**
```bash
# EC2 IP アドレス確認
aws ec2 describe-instances --instance-ids i-xxxxxxxxx \
  --query 'Reservations[0].Instances[0].PublicIpAddress'

# GitHub Secrets を更新
gh secret set EC2_HOST --body "正確なIP"
```

### Q: "Connection timed out"エラー

**原因:** セキュリティグループが SSH を許可していない

**対応:**
```bash
# EC2 セキュリティグループを確認
aws ec2 describe-security-groups --group-ids sg-xxxxxxxxx

# Actions専用SGへのランナーIPv4/32一時許可が成功したか確認する。
# 0.0.0.0/0や::/0でのSSH公開は行わない。
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
