# Final Tax Return

所得税確定申告用のWebアプリケーション

## 技術スタック

- **言語**: Java 23
- **フレームワーク**: Spring MVC 6.2.10
- **ビルドツール**: Maven
- **ビューテンプレート**: JSP
- **データベース**: MySQL 8.0
- **アプリケーションサーバー**: Apache Tomcat 10.1.50
- **ORM**: Spring Data JPA / Hibernate

## プロジェクト構成

```
src/
├── main/
│   ├── java/org/example/     # Java ソースコード
│   │   ├── controller/       # Spring Controller
│   │   ├── service/          # ビジネスロジック
│   │   └── repository/       # データアクセス層
│   └── resources/
│       ├── application.properties  # アプリケーション設定
│       ├── css/              # スタイルシート
│       └── js/               # JavaScriptファイル
└── test/                     # テストコード
```

## 機能

- 収益登録（複数の収益区分に対応）
- 費用登録（経費管理）
- 損益レポート（検索機能付き）

## セットアップ

### 前提条件
- Java 23以上
- Maven 3.6以上
- MySQL 8.0以上
- Tomcat 10.1以上

### インストール

```bash
# リポジトリをクローン
git clone https://github.com/your-username/final-tax-return.git
cd final-tax-return

# Mavenでビルド
mvn clean install

# WARファイルがtarget/final-tax-return-1.0-SNAPSHOT.warに生成される
```

### デプロイ

```bash
# WARファイルをTomcatのwebappsディレクトリにコピー
cp target/final-tax-return-1.0-SNAPSHOT.war /usr/local/tomcat/apache-tomcat-10.1.50/webapps/

# Tomcatを再起動
/usr/local/tomcat/apache-tomcat-10.1.50/bin/shutdown.sh
/usr/local/tomcat/apache-tomcat-10.1.50/bin/startup.sh
```

## データベース設定

`src/main/resources/application.properties`でMySQL接続情報を設定してください。

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/final_tax_return
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## ライセンス

MIT

## 作者

[Your Name]
