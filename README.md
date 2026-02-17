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

### 2. Mavenでビルド

```bash
mvn clean install
```

ビルド完了後、`target/final-tax-return-1.0-SNAPSHOT.war`が生成されます。

### 3. データベース設定

`src/main/resources/application.properties`を編集：

```properties
# MySQL接続設定
spring.datasource.url=jdbc:mysql://localhost:3306/final_tax_return
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate設定
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### 4. Tomcatへのデプロイ

```bash
# WARファイルをTomcatのwebappsディレクトリにコピー
cp target/final-tax-return-1.0-SNAPSHOT.war /usr/local/tomcat/apache-tomcat-10.1.50/webapps/

# Tomcatを再起動
/usr/local/tomcat/apache-tomcat-10.1.50/bin/shutdown.sh
/usr/local/tomcat/apache-tomcat-10.1.50/bin/startup.sh
```

### 5. アプリケーションにアクセス

```
http://localhost:8080/final-tax-return-1.0-SNAPSHOT
```

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

## 📝 ライセンス

MIT License

## 👤 作成者

Tsuyoshi Shoji

## 🔗 リポジトリ

https://github.com/Tsuyoshi-Shoji/tax-return
