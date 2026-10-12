# GitHub Secrets セットアップガイド

## 概要

本番環境へのデプロイに必要な GitHub Secrets を設定します。

## 必須 Secrets

### 1. EC2_HOST
**説明**: EC2 インスタンスのパブリック IP またはドメイン名

**取得方法**:
```bash
# AWS Management Console から確認
# または EC2 インスタンス詳細 > パブリック IP アドレス
```

**例**: `54.123.45.67` または `ec2-instance.example.com`

### 2. EC2_PRIVATE_KEY
**説明**: EC2 キーペアの秘密鍵

**取得方法**:
```bash
# キーペア作成時にダウンロードした .pem ファイルの内容
cat /path/to/your-key-pair.pem
```

**重要** ⚠️:
- 秘密鍵は絶対に Git リポジトリに含めない
- GitHub Secrets として安全に保存される
- ローカルマシンでは `chmod 600 your-key-pair.pem` でパーミッション設定

## 設定手順

### GitHub リポジトリ上での設定

1. **リポジトリページにアクセス**
   - `https://github.com/Tsuyoshi-Shoji/tax-return`

2. **Settings タブをクリック**
   - リポジトリの右上 > Settings

3. **Secrets and variables → Actions をクリック**
   - 左メニュー > Security > Secrets and variables > Actions

4. **New repository secret をクリック**

5. **EC2_HOST を追加**
   - Name: `EC2_HOST`
   - Secret: `EC2 インスタンスのパブリック IP`
   - Add secret をクリック

6. **EC2_PRIVATE_KEY を追加**
   - Name: `EC2_PRIVATE_KEY`
   - Secret: 秘密鍵ファイルの内容（全文コピペ）
   - Add secret をクリック

## 検証方法

GitHub リポジトリに push してワークフローが実行されるか確認：

```bash
# ローカルで変更をコミット
git add .
git commit -m "chore: GitHub Secrets 設定完了"
git push origin main

# GitHub Actions ページでワークフロー実行を確認
# https://github.com/Tsuyoshi-Shoji/tax-return/actions
```

## トラブルシューティング

### 認証エラー: "Permission denied (publickey)"
- EC2_PRIVATE_KEY が正しく設定されているか確認
- 秘密鍵の先頭 `-----BEGIN RSA PRIVATE KEY-----` から最後 `-----END RSA PRIVATE KEY-----` までをコピー

### ホスト接続エラー: "Could not resolve hostname"
- EC2_HOST の値が正しいか確認（IP またはドメイン名）
- EC2 インスタンスが起動状態か確認

### デプロイ完了後のヘルスチェック失敗
- EC2 セキュリティグループで HTTP (8080) が許可されているか確認
- EC2 インスタンスで Tomcat が起動しているか確認
  ```bash
  ssh -i your-key.pem ec2-user@<EC2_HOST>
  tail -f /var/log/deploy.log
  ```

## セキュリティ考慮事項

- ✅ GitHub Secrets は暗号化される
- ✅ ログには Secrets が表示されない
- ⚠️ Secrets へのアクセス権限を制限（collaborator 管理）
- ⚠️ 秘密鍵が漏えいした場合、EC2 キーペアを再作成
