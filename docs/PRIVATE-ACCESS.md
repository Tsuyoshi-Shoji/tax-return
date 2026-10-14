# 私用端末限定アクセス（WireGuard + HTTPS）

## 方針と適用範囲

スマートフォンと私用PCを端末ごとに別のWireGuard鍵で登録する。モバイルルーターはインターネット回線として使い、VPNは端末側で接続する。ルーター全体をVPNに登録すると、その配下の他端末にもアクセスを渡してしまうため、既定では登録しない。

```text
私用PC      10.77.0.2 ─┐
iOS         10.77.0.3 ─┼ WireGuard（UDP 51820）
Android     10.77.0.4 ─┘
                          EC2 wg0: 10.77.0.1
                            └ HTTPS :443 / Nginx
                                └ 127.0.0.1:8080 / Tomcat
                                    └ localhost / DB
```

公開するのはVPNのUDPポートだけ。HTTP/HTTPS、Tomcat、DBをインターネットへ公開しない。VPN認証に加えアプリのログインも必要。IP転送・NAT・端末間ルーティングは不要で、有効にしない。

この手順はAmazon Linux 2023、専用EC2、既定の `/opt/tomcat` を対象とする。設定例の追加だけでは本番に適用されない。既存ホストでは設定バックアップ、保守時間、SSH復旧経路を確保してから手動適用する。`setup-ec2.sh` はDB初期化を含むため、既存ホストへの再実行は禁止。

## 1. 公開範囲を先に閉じる

- AWSセキュリティグループからTCP 80・443・8080・3306の公開ルールを削除する。IPv4・IPv6、全ての付与グループ、ロードバランサー経由も確認する。
- SSHは管理者の接続元だけに限定する。動的IP環境では、後述のVPN確立後にVPN経由管理へ移す。変更中のSSH接続を失わないこと。
- UDP 51820を追加する。モバイル回線のIP変更を許容する場合、このUDPポートだけは任意の接続元を許可する。未登録鍵にはアプリへの接続権限がないが、VPNサービス自体のDoS・更新管理は必要。
- 安定したEC2接続先（Elastic IP等）を用意する。既存LAN/VPNが `10.77.0.0/24` と重複する場合は、全設定例のアドレスを整合的に変更する。

## 2. WireGuardを設定する

EC2で必要なパッケージを導入する。秘密鍵をチャット、Git、ログ、GitHub Secretsへコピーしない。

```bash
sudo dnf install -y wireguard-tools nginx
sudo install -d -m 700 /etc/wireguard
sudo sh -c 'umask 077; wg genkey > /etc/wireguard/server.key; wg pubkey < /etc/wireguard/server.key > /etc/wireguard/server.pub'
sudo install -m 600 scripts/private-access/wg0.conf.example /etc/wireguard/wg0.conf
sudoedit /etc/wireguard/wg0.conf
```

`wg0.conf` のサーバー秘密鍵プレースホルダーをEC2の鍵に置換する。Peerには登録する各端末の**公開鍵**を記入する。端末秘密鍵は各端末内で作成し、他端末と共有しない。スマートフォンは公式WireGuardアプリで新規トンネルを作成する。

PC用設定は [client.conf.example](../scripts/private-access/client.conf.example) を使う。iOSは `Address = 10.77.0.3/32`、Androidは `Address = 10.77.0.4/32` にする。各端末の `PrivateKey`、サーバー `PublicKey`、EC2の `Endpoint` を設定する。`AllowedIPs = 10.77.0.1/32` によりアプリ宛てだけをVPNに流す。

```bash
sudo systemctl enable --now wg-quick@wg0
sudo wg show wg0
```

`wg showconf` は秘密鍵を表示するため診断ログに使用しない。鍵紛失時は該当Peerを削除して接続を失効させ、アプリのセッションも無効化する。

## 3. VPN内HTTPS用証明書

ブラウザーが信頼する証明書を `10.77.0.1` のIP SAN付きで用意する。私用CAを使う場合、CA秘密鍵は管理用PCの暗号化された保管先に置き、EC2には置かない。各私用端末にCA証明書を信頼設定する。スマートフォンではルート証明書の明示的な信頼設定が必要な場合がある。警告の無視や `curl -k` を通常運用にしない。

管理用PCの、Git管理外の暗号化されたディレクトリでの作成例：

```bash
umask 077
openssl genpkey -algorithm RSA -aes-256-cbc -pkeyopt rsa_keygen_bits:3072 -out private-ca.key
openssl req -x509 -new -sha256 -days 3650 -key private-ca.key -out private-ca.crt \
  -subj '/CN=Private Tax Return CA' \
  -addext 'basicConstraints=critical,CA:TRUE' \
  -addext 'keyUsage=critical,keyCertSign,cRLSign'
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out server.key
openssl req -new -key server.key -out server.csr -subj '/CN=10.77.0.1'
printf '%s\n' 'subjectAltName=IP:10.77.0.1' 'basicConstraints=critical,CA:FALSE' \
  'keyUsage=critical,digitalSignature,keyEncipherment' 'extendedKeyUsage=serverAuth' > server.ext
openssl x509 -req -in server.csr -CA private-ca.crt -CAkey private-ca.key -CAcreateserial \
  -out server.crt -days 365 -sha256 -extfile server.ext
openssl verify -CAfile private-ca.crt -verify_ip 10.77.0.1 server.crt
```

SSH等の保護された経路で `server.crt` と `server.key` だけをEC2へ転送する。EC2では証明書を `/etc/nginx/tax-return/server.crt`（root所有、644）、秘密鍵を `/etc/nginx/tax-return/server.key`（root所有、600）に配置する。ディレクトリはroot所有、700とする。転送用コピーは不要になった時点で削除する。証明書更新日・期限監視を登録し、期限前に更新する。

## 4. TomcatとNginxを私用構成にする

以下は専用ホスト用の置換設定。既存のConnector・仮想ホスト等がある場合はそのまま上書きせず、差分を統合する。既存設定を個別にバックアップしてから適用する。

```bash
sudo cp -p /opt/tomcat/conf/server.xml /opt/tomcat/conf/server.xml.before-private
sudo cp -p /opt/tomcat/conf/context.xml /opt/tomcat/conf/context.xml.before-private
sudo cp -p /etc/nginx/nginx.conf /etc/nginx/nginx.conf.before-private
sudo install -o root -g tomcat -m 640 scripts/private-access/tomcat-server.xml /opt/tomcat/conf/server.xml
sudo install -o root -g tomcat -m 640 scripts/private-access/tomcat-context.xml /opt/tomcat/conf/context.xml
sudo install -o root -g tomcat -m 750 scripts/tomcat-setenv.sh /opt/tomcat/bin/setenv.sh
sudo install -o root -g root -m 644 scripts/private-access/nginx.conf /etc/nginx/nginx.conf
sudo install -o root -g root -m 644 scripts/private-access/nginx-tax-return.conf /etc/nginx/conf.d/tax-return.conf
sudo nginx -t
```

NginxはVPNアドレスだけで待ち受け、許可する送信元もPC・スマートフォンの2アドレスだけ。Tomcatはloopbackだけで待ち受け、NginxからのHTTPSを `scheme=https, secure=true, proxyPort=443` で扱う。セッションCookieはSecure・HttpOnly・SameSite=Strictになる。ローカルHTTPのヘルスチェックは可能だが、ローカルHTTPでのブラウザーログインはサポートしない。

Nginxの起動をVPNに依存させる：

```bash
sudo install -d -m 755 /etc/systemd/system/nginx.service.d
printf '%s\n' '[Unit]' 'Requires=wg-quick@wg0.service' 'After=wg-quick@wg0.service' \
  | sudo tee /etc/systemd/system/nginx.service.d/wireguard.conf > /dev/null
sudo systemctl daemon-reload
```

firewalldが有効なら、**実際の外向きインターフェースのzone**から80・443・8080・3306のport/service許可を取り除く。既存のrich rule・policyによる許可も確認する。`wg0` 用の専用zoneを作り443/tcpだけを許可し、外向きzoneでは51820/udpだけを追加する。新規zoneの例：

```bash
sudo firewall-cmd --permanent --new-zone=tax-vpn
sudo firewall-cmd --permanent --zone=tax-vpn --add-interface=wg0
sudo firewall-cmd --permanent --zone=tax-vpn --add-port=443/tcp
sudo firewall-cmd --reload
sudo firewall-cmd --get-active-zones
sudo firewall-cmd --list-all-zones
```

`--new-zone` は初回のみ。firewalld非使用時は同じ方針を既存のnftables等で実装する。SELinuxによるloopbackプロキシの拒否があれば、監査ログを確認して必要な許可を追加し、SELinux自体を無効化しない。

Tomcatは既存の運用方式で停止・再起動する（このプロジェクトのデプロイは `catalina.sh` を使用）。新WARもデプロイした後、Nginxを開始する：

```bash
sudo systemctl enable --now nginx
```

## 5. 適用後の確認（必須）

- `ss -lnt` でTomcatが `127.0.0.1:8080` のみ、HTTPSが `10.77.0.1:443` のみになっていること。公開アドレスや `0.0.0.0` / `[::]` のHTTP待受がないこと。
- VPNを有効にした各許可端末で `https://10.77.0.1/final-tax-return-1.0-SNAPSHOT/login` に接続し、証明書警告なし、ログイン・帳簿操作が成功すること。
- ブラウザーでJSESSIONIDのSecure・HttpOnly・SameSite=Strictを確認する。
- VPNを切った端末・未登録端末からアプリへ接続できないこと。自分が管理する公開IPの80・443・8080・3306も外部から到達できないこと。
- OS再起動後もVPN→Nginxの順で起動すること。VPN停止後にNginxも停止すること。VPN再起動後はNginxを再起動する。
- Javaのプロセス引数にDBパスワードがないこと。プロセス環境には残るため、root・Tomcat実行ユーザーには専用権限を与え、他用途のプロセスを同じユーザーで動かさない。
- 既存のデモアカウントをDB管理者が確認し、実データがあれば退避・所有者確認した後で無効化する。今回のコード変更は既存アカウントを自動削除しない。

## 残る対策・復旧

VPNは保存データの暗号化、端末侵害、DB権限窃取、MFA、依存関係の更新を代替しない。EBS・バックアップ暗号化、失効済みセッションの扱い、OS/パッケージ更新は別途実設定を確認する。

障害時はSSH管理経路からVPN・Nginx・Tomcatの状態を確認し、必要ならバックアップした設定へ戻す。復旧のためにアプリの公開ポートを開けない。変更・確認結果・実施者を運用記録に残す。

公式仕様確認（2026-10-13）:
- [WireGuard Quick Start](https://www.wireguard.com/quickstart/)
- [Tomcat HTTP Connector](https://tomcat.apache.org/tomcat-11.0-doc/config/http.html)
- [Tomcat Cookie Processor](https://tomcat.apache.org/tomcat-11.0-doc/config/cookie-processor.html)
