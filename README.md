# 所得税確定申告支援システム（Tax Return）

事業所得の確定申告を効率的に行うためのWebアプリケーションです。収益・経費の登録から損益レポートの生成まで、一連の申告準備業務をサポートします。

## 📋 主な機能

### 1. 収益管理
- 複数の勘定項目で収益を分類登録
- 日付、金額、詳細情報を入力
- カレンダーUIで日付入力をサポート
- 金額の自動カンマ区切り表示

### 2. 経費管理
- 事業経費を勘定項目別に登録
- 日付、金額、但し書きを記録
- 収益管理と同じUIで統一されたUX

### 3. 損益レポート
- 期間指定での検索機能
- 収益/経費の区分フィルタ
- 勘定項目の複数選択対応
- 損益結果の可視化

## 🛠️ 技術スタック

| 項目 | 内容 |
|------|------|
| **言語** | Java 23 |
| **フレームワーク** | Spring MVC 6.2.10 |
| **ビルドツール** | Maven |
| **ビューテンプレート** | JSP |
| **データベース** | MySQL 8.0 |
| **アプリケーションサーバー** | Apache Tomcat 10.1.50 |
| **ORM** | Spring Data JPA / Hibernate |
| **フロントエンド** | HTML5 / CSS3 / JavaScript |

## 📁 プロジェクト構成

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── Main.java                    # アプリケーション起動クラス
│   │   ├── controller/                  # Spring Controller
│   │   │   └── HomeController.java
│   │   ├── service/                     # ビジネスロジック層
│   │   │   ├── HomeService.java
│   │   │   └── impl/
│   │   │       └── HomeServiceImpl.java
│   │   └── config/                      # Spring設定
│   │       ├── WebAppInitializer.java
│   │       └── WebConfig.java
│   ├── resources/
│   │   ├── application.properties        # アプリケーション設定
│   │   ├── css/
│   │   │   └── style.css                # スタイルシート
│   │   └── js/
│   │       └── app.js                   # JavaScriptスクリプト
│   └── webapp/WEB-INF/views/            # JSPテンプレート
│       ├── header.jsp                   # ヘッダー
│       ├── footer.jsp                   # フッター
│       ├── home.jsp                     # ホーム画面
│       ├── income.jsp                   # 収益登録画面
│       ├── expense.jsp                  # 経費登録画面
│       └── profit-loss.jsp              # 損益レポート画面
└── test/java/                           # テストコード
```

## 🚀 セットアップ・実行方法

### 前提条件
- Java 23以上
- Maven 3.6以上
- MySQL 8.0以上
- Apache Tomcat 10.1以上

### 1. リポジトリをクローン

```bash
git clone https://github.com/Tsuyoshi-Shoji/tax-return.git
cd tax-return
```

### 2. 環境の準備（.gitignore に記載されるファイルの復元）

`.gitignore` に記載されているファイルやディレクトリは、以下のコマンドで自動生成または復元されます。

#### 2-1. IDEの設定ファイル復元

プロジェクトをIDEで開くと、自動的に生成されます：

- **IntelliJ IDEA**: プロジェクトを開くと `.idea/` ディレクトリが自動生成
- **Eclipse**: プロジェクトを開くと `.classpath`, `.project` などが自動生成
- **VS Code**: 拡張機能導入時に `.vscode/` が自動生成

#### 2-2. ビルド成果物の生成

```bash
# Maven依存関係のダウンロード（初回のみ必須）
mvn dependency:resolve
```

このコマンドにより、`~/.m2/repository/` に Maven の依存ライブラリがダウンロード・キャッシュされます。

### 3. Mavenでビルド

```bash
mvn clean install
```

このコマンドにより以下が自動実行されます：

- **依存関係のダウンロード**: Maven Central Repository から全てのライブラリを取得
- **コンパイル**: Java ソースコードをコンパイル  
- **テスト実行**: テストコードを実行
- **ビルド成果物の生成**:
  - `target/` ディレクトリ
  - `target/final-tax-return-1.0-SNAPSHOT.war`（デプロイ用のWARファイル）
  - `target/classes/`（コンパイル済みクラス）
  - その他のビルド関連ファイル

> **注意**: `target/` ディレクトリは `.gitignore` に記載されているため、クローン時には含まれません。ビルド時に自動生成されます。

### 4. データベース設定

`src/main/resources/application.properties` を編集し、接続先を指定します。

#### 4-1. ローカルMySQL環境での設定

```properties
# MySQL接続設定
spring.datasource.url=jdbc:mysql://localhost:3306/final_tax_return
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate設定
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

#### 4-2. AWS RDS（MySQL）での設定

```properties
# AWS RDS接続設定
spring.datasource.url=jdbc:mysql://my-database-1.cyxeeww4gysa.us-east-1.rds.amazonaws.com:3306/final_tax_return?useSSL=true&serverTimezone=UTC
spring.datasource.username=admin
spring.datasource.password=shoji0409
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# SSL証明書の設定（オプション）
# server.ssl.key-store=/path/to/keystore.p12
# server.ssl.key-store-password=password

# Hibernate設定
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

> **注意**: パスワードは `.gitignore` に記載されているため、リポジトリにはコミットされません。クローン後、自分の環境に合わせて設定してください。

### 5. Tomcatへのデプロイ

#### 5-1. WARファイルのコピーとデプロイ

```bash
# WARファイルをTomcatのwebappsディレクトリにコピー
cp target/final-tax-return-1.0-SNAPSHOT.war /usr/local/tomcat/apache-tomcat-10.1.50/webapps/
```

#### 5-2. Tomcatの再起動

```bash
# Tomcatの停止
/usr/local/tomcat/apache-tomcat-10.1.50/bin/shutdown.sh

# Tomcatの起動
/usr/local/tomcat/apache-tomcat-10.1.50/bin/startup.sh

# ログの確認（オプション）
tail -f /usr/local/tomcat/apache-tomcat-10.1.50/logs/catalina.out
```

> **TIP**: 初回デプロイ時は WARファイルの展開に少し時間がかかります。ログで確認してください。

### 6. アプリケーションにアクセス

```
http://localhost:8080/final-tax-return-1.0-SNAPSHOT
```

## ⚡ クイックスタート

以下のコマンドを順に実行すれば、セットアップからデプロイまで完了します：

```bash
# 1. リポジトリをクローン
git clone https://github.com/Tsuyoshi-Shoji/tax-return.git
cd tax-return

# 2. Maven依存関係をダウンロード
mvn dependency:resolve

# 3. ビルド（WARファイル生成）
mvn clean install

# 4. application.properties を編集（自分の環境に合わせて）
# エディタで以下のファイルを開き、DB接続情報を設定
# src/main/resources/application.properties

# 5. Tomcatへデプロイ
cp target/final-tax-return-1.0-SNAPSHOT.war /usr/local/tomcat/apache-tomcat-10.1.50/webapps/

# 6. Tomcatを再起動
/usr/local/tomcat/apache-tomcat-10.1.50/bin/shutdown.sh
/usr/local/tomcat/apache-tomcat-10.1.50/bin/startup.sh

# 7. アプリケーションにアクセス
# ブラウザで以下にアクセス
# http://localhost:8080/final-tax-return-1.0-SNAPSHOT
```

## 📋 .gitignore に記載されるファイル一覧

| カテゴリ | ファイル/ディレクトリ | 説明 |
|---------|---------------------|------|
| **ビルド成果物** | `target/` | Maven ビルド出力（自動生成） |
| **IDE設定** | `.idea/`, `*.iml`, `*.ipr` | IntelliJ IDEA 設定 |
| | `.classpath`, `.project` | Eclipse 設定 |
| | `.vscode/` | Visual Studio Code 設定 |
| | `*.iws` | IntelliJ ワークスペース |
| **ビルド中間ファイル** | `.apt_generated`, `.sts4-cache` | アノテーション処理ファイル |
| | `/nbproject/`, `/nbbuild/` | NetBeans ビルド出力 |
| | `/dist/` | 配布用ディレクトリ |
| **OS ファイル** | `.DS_Store` | macOS ファイル |

> 💡 **ポイント**: これらのファイルは不要な容量を削減するため、リポジトリに含めません。クローン後、ビルドコマンド実行時に自動生成されます。

## 📱 UI・UX特徴

- **統一されたレイアウト**: 収益・経費登録画面は同じUIで実装
- **カレンダーピッカー**: 日付入力時にカレンダーを表示
- **自動フォーマット**: 金額入力時に日本円のカンマ区切りを自動適用
- **レスポンシブデザイン**: ヘッダー・フッター付きで、各画面のコンテンツはセンタリング表示
- **バリデーション**: 日付範囲チェック、数値入力の検証

## 🔄 使用フロー

1. **ホーム画面**で機能を選択
2. **収益登録画面**で収入を登録
   - 勘定項目を選択
   - 日付を入力
   - 金額を入力
   - 詳細を記入
3. **経費登録画面**で支出を登録
   - 勘定項目を選択
   - 日付を入力
   - 金額を入力
   - 但し書きを記入
4. **損益レポート画面**で集計結果を確認
   - 期間を指定
   - 収益/経費を選択
   - 勘定項目でフィルタリング
   - 検索して結果を表示

## 🔧 トラブルシューティング

### ❌ ビルドに失敗する

**エラー**: `Could not find java.lang.String`

**原因**: Java のバージョンが不適切

**解決策**:
```bash
# Java バージョン確認
java -version

# Java 23 以上がインストールされているか確認
# 必要に応じてインストール
```

---

### ❌ データベース接続エラー

**エラー**: `Access denied for user 'admin'@'localhost'`

**原因**: `application.properties` のDB接続情報が間違っている

**解決策**:
```bash
# application.properties を編集
vi src/main/resources/application.properties

# 以下を確認:
# - spring.datasource.url: 正しいホスト/ポート/DB名
# - spring.datasource.username: 正しいユーザー名
# - spring.datasource.password: 正しいパスワード
```

---

### ❌ Tomcat に404エラー

**エラー**: `HTTP Status 404 - Not Found`

**原因**: アプリケーションが正しくデプロイされていない

**解決策**:
```bash
# 1. Tomcat ログを確認
tail -f /usr/local/tomcat/apache-tomcat-10.1.50/logs/catalina.out

# 2. WARファイルが正しくコピーされているか確認
ls -la /usr/local/tomcat/apache-tomcat-10.1.50/webapps/

# 3. Tomcat を再起動
/usr/local/tomcat/apache-tomcat-10.1.50/bin/shutdown.sh
/usr/local/tomcat/apache-tomcat-10.1.50/bin/startup.sh

# 4. 少し待ってからアクセス（展開に時間がかかる場合がある）
sleep 10
# ブラウザでアクセス: http://localhost:8080/final-tax-return-1.0-SNAPSHOT
```

---

### ❌ Maven 依存関係がダウンロードできない

**エラー**: `Could not transfer artifact`

**原因**: Maven Central Repository に接続できない、またはネットワーク問題

**解決策**:
```bash
# Maven キャッシュをクリア
rm -rf ~/.m2/repository

# 依存関係を再ダウンロード
mvn dependency:resolve -U

# またはビルドを再実行
mvn clean install
```

---

### ❌ IDE で `target/` ディレクトリが見つからない

**原因**: ビルドがまだ実行されていない

**解決策**:
```bash
# ビルドを実行
mvn clean install

# IDE をリフレッシュ
# IntelliJ IDEA: View → Reload File from Disk
# Eclipse: F5 キーを押してリフレッシュ
```

## 📝 ライセンス

MIT License

## 👤 作成者

Tsuyoshi Shoji

## 🔗 リポジトリ

https://github.com/Tsuyoshi-Shoji/tax-return
