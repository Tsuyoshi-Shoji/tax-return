# GitHub Actionsからの一時SSH許可

## 動作

`main`へのpushでWARをビルドし、GitHub OIDCによる短期AWS認証を取得する。
ランナーの送信元パブリックIPv4 `/32` だけをTCP 22に許可し、SSHで
`/opt/deploy.sh` を実行する。ヘルスチェックはEC2内の
`127.0.0.1:8080/final-tax-return-1.0-SNAPSHOT/login`で行う。
8080番の公開、既存の管理用SSHルールの変更はしない。

本変更はSSH接続経路の変更であり、既存のデプロイスクリプトによるEC2上の
再ビルド方式は維持する。GitHubでビルドしたWARの転送方式への変更は含まない。

## 1. AWSで準備するもの

AWS管理権限を持つ担当者が実施する。EC2へのAWS管理用アクセスキーの配置は不要。

1. 対象EC2にパブリックIPv4とインターネットへの経路があることを確認する。
   安定した接続先にはElastic IPを使う。IPv6のみの接続先は今回の方式では使用しない。
2. EC2と同じVPCに、Actions専用のセキュリティグループを新規作成する。
   インバウンドは空で開始する。既存のセキュリティグループを外さず、このSGを追加する。
   SGはこのEC2だけに関連付け、他のインスタンスと共有しない。
3. ネットワークACL・OSのファイアウォール・sshdでもSSH通信が成立することを確認する。
   SGへの一時許可だけでは、他のレイヤーの拒否は解除されない。
4. IAMにOIDCプロバイダーを作成する（存在する場合は再利用）。
   - URL: `https://token.actions.githubusercontent.com`
   - Audience: `sts.amazonaws.com`
5. `GitHubTaxReturnDeploy`等のIAMロールを作成し、以下の信頼ポリシーと権限を設定する。

### ロールの信頼ポリシー

`ACCOUNT_ID`をAWSアカウントIDに置換する。ワイルドカードで他のブランチや
リポジトリまで許可しない。

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:Tsuyoshi-Shoji/tax-return:ref:refs/heads/main"
      }
    }
  }]
}
```

GitHubでimmutable subject claimsを使用している場合、`sub`には所有者・リポジトリの
数値IDも含まれる。上記をそのまま使わず、リポジトリで実際に使用するsubjectに一致させる。
GitHub Environmentはこのワークフローでは使用しない。後から導入する場合は信頼条件も変更する。

### ロールの権限ポリシー

`REGION`、`ACCOUNT_ID`、`SECURITY_GROUP_ID`を実値に置換する。
指定するSGは、上で作ったActions専用SGに限定する。

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ec2:DescribeSecurityGroupRules",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "ec2:AuthorizeSecurityGroupIngress",
      "Resource": "arn:aws:ec2:REGION:ACCOUNT_ID:security-group/SECURITY_GROUP_ID",
      "Condition": {
        "StringEquals": {
          "ec2:Region": "REGION"
        }
      }
    },
    {
      "Effect": "Allow",
      "Action": "ec2:AuthorizeSecurityGroupIngress",
      "Resource": "arn:aws:ec2:REGION:ACCOUNT_ID:security-group-rule/*",
      "Condition": {
        "StringEquals": {
          "aws:RequestTag/ManagedBy": "github-actions-ssh",
          "aws:RequestTag/Repository": "Tsuyoshi-Shoji/tax-return"
        }
      }
    },
    {
      "Effect": "Allow",
      "Action": "ec2:RevokeSecurityGroupIngress",
      "Resource": "arn:aws:ec2:REGION:ACCOUNT_ID:security-group/SECURITY_GROUP_ID"
    },
    {
      "Effect": "Allow",
      "Action": "ec2:CreateTags",
      "Resource": "arn:aws:ec2:REGION:ACCOUNT_ID:security-group-rule/*",
      "Condition": {
        "StringEquals": {
          "ec2:CreateAction": "AuthorizeSecurityGroupIngress",
          "aws:RequestTag/ManagedBy": "github-actions-ssh",
          "aws:RequestTag/Repository": "Tsuyoshi-Shoji/tax-return"
        }
      }
    }
  ]
}
```

Describeはリソース単位の制限ができないため`*`とする。変更操作は専用SGに限定する。
`AuthorizeSecurityGroupIngress`はSG側と作成ルール側の許可を分ける。
`aws:RequestTag`はルール側で評価されるため、SG側の許可には付けない。
ルールARNは作成前にIDが決まらないため`security-group-rule/*`とし、
同じリクエストで必要となるSG側の許可を専用SGに限定する。
このポリシーだけでTCP 22や`/32`を強制できるわけではなく、これらはスクリプトで検証する。
ロールは対象SGの他のインバウンドルールも削除できるため、管理用SGとの共用は禁止する。
`main`のブランチ保護・変更レビューを設定し、ワークフローの編集権限も管理する。

## 2. GitHubの設定

リポジトリのSettings → Secrets and variables → Actionsで登録する。

### Variables

| 名前 | 値 |
| --- | --- |
| `AWS_REGION` | EC2のリージョン（本EC2は`ap-northeast-1`） |
| `AWS_DEPLOY_ROLE_ARN` | 作成したOIDCロールのARN |
| `EC2_SECURITY_GROUP_ID` | Actions専用SGのID（`sg-...`） |

### Secrets

| 名前 | 値 |
| --- | --- |
| `EC2_HOST` | EC2のパブリックIPv4のみ。URL・ポート・角括弧は付けない |
| `EC2_PRIVATE_KEY` | `ec2-user`へのSSH接続用秘密鍵の全文 |
| `EC2_KNOWN_HOSTS` | `EC2_HOST ssh-ed25519 公開ホスト鍵`の1行 |

`EC2_KNOWN_HOSTS`は、信頼済みのEC2ターミナルで次を実行して生成する。
入力するIPv4は`EC2_HOST`と同じ値にする。指紋（`SHA256:...`）ではなく出力の1行全体を登録する。

```bash
read -r -p 'EC2_HOSTのパブリックIPv4: ' DEPLOY_HOST
printf '%s ' "$DEPLOY_HOST"
sudo awk '{print $1, $2}' /etc/ssh/ssh_host_ed25519_key.pub
```

秘密鍵・AWS認証情報・OIDCトークンをチャットやGitへ貼り付けない。
GitHubのAWS認証用に長期アクセスキーを登録する必要はない。

## 3. EC2側の確認と有効化

- `/opt/deploy.sh`がroot所有で、`ec2-user`から`sudo -n /opt/deploy.sh`を実行できること。
- デプロイ対象リポジトリに未コミット変更がないこと。変更を破棄して回避しない。
- EC2にJava 21・Mavenがあり、既存のデプロイとDB移行が正常動作すること。
- 設定ファイルをレビューしてコミット・pushする。設定不足のままpushすると事前検証で停止する。
- 初回はActionsの各ステップとAWSのSGルールを確認し、完了後に一時許可が消えることを確認する。
- ルール追加・削除・拒否をCloudTrailで確認できるようにし、失敗通知の受信担当者を決める。

## 4. 削除漏れ対策

ルールには`ManagedBy`、`Repository`、`RunId`、`ExpiresAt`のタグを作成時に付ける。
`RunId`は実行IDと再実行番号の組で、追加APIの応答が失われても終了処理で検索できる。
終了処理は成功・失敗・通常キャンセル時に実行し、その実行のルールだけを削除する。
既存の許可と同じCIDRで重複した場合は追加を失敗させ、既存ルールを流用・削除しない。

**SG自体にはTTL機能がなく、`ExpiresAt`タグだけでは自動削除されない。**
デプロイは最大30分、一時許可の期限は追加から1時間とする。
次のpush実行前、および15分間隔の定期ジョブで期限切れの管理対象ルールを回収する。
Actions画面の手動実行（Run workflow）は回収ジョブのみを実行し、デプロイはしない。
デプロイと回収は同じconcurrencyグループで直列化する。

GitHubの定期実行は遅延・欠落する可能性があり、厳密に1時間で閉じる保証はない。
強制キャンセル、AWS/GitHub障害、OIDC失効で削除できない場合は、運用担当者が専用SGの
`ManagedBy=github-actions-ssh`タグと`ExpiresAt`を確認し、期限切れルールを手動削除する。
定期実行失敗の通知と定期実行自体の停止（非活動による無効化等）を監視する。
厳密な閉鎖期限が必要なら、GitHubとは独立したAWS側の定期回収が別途必要。

## 検証

```bash
python3 -B -m unittest discover -s scripts/ci -p 'test_*.py' -v
```

テストはAWS APIをモックし、TCP 22・IPv4 `/32`・期限・実行単位の削除・範囲外ルールの拒否・
AWSエラーの伝播を検証する。実環境のOIDC、IAM、SG反映、SSH、削除は初回実行で確認する。
ロールとGitHub Variables/Secretsが未設定の状態では自動デプロイは利用できない。

参照:
- [GitHub OIDC / AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
- [AWS CLI / AuthorizeSecurityGroupIngress](https://docs.aws.amazon.com/cli/latest/reference/ec2/authorize-security-group-ingress.html)
