# ファイナンスマネジメント

## プロジェクト概要

**tax-return** は、個人事業主や自営業者向けの**収支管理および確定申告書作成・管理Webアプリケーション**です。収入・経費の登録から損益計算、申告書生成まで、一連の税務処理をサポートします。

### 主な特徴
- **ユーザーフレンドリーなUI**: 複雑な税務手続きを直感的に実行
- **リアルタイム計算**: 収入・経費の登録に基づいた自動損益計算
- **申告書生成**: 登録データから申告書を自動生成
- **設定管理**: ユーザー情報や税務設定のカスタマイズ

---

## 利用技術スタック

### バックエンド
| 技術 | バージョン | 役割 |
|------|----------|------|
| **Java** | 23 | プログラミング言語 |
| **Spring MVC** | 6.2.10 | Webフレームワーク |
| **Spring Boot** | 3.2.1 | アプリケーション基盤（JPA/データソース管理） |
| **Hibernate/JPA** | 6.4.1 | ORM（オブジェクト関係マッピング） |
| **MySQL** | 8.0.33 | データベース |
| **HikariCP** | 5.0.1 | コネクションプーリング |

### フロントエンド
| 技術 | バージョン | 役割 |
|------|----------|------|
| **JSP** | 3.1.1 | Javaサーバーページ |
| **JSTL** | 3.0.0+ | JSPタグライブラリ |
| **HTML5 + CSS3** | - | マークアップ・スタイリング |
| **JavaScript** | - | クライアント側の動的処理 |

### ビルド・デプロイ
| ツール | 役割 |
|------|------|
| **Maven** | ビルド・依存関係管理 |
| **Tomcat** | 11.0.21 | Webサーバー/アプリケーションサーバー |

---

## 機能概要

### 📊 主要機能

| 機能 | 説明 | URL |
|------|------|------|
| **ホーム** | ダッシュボード・ナビゲーション | `/` |
| **申告書作成** | 確定申告書の作成・編集 | `/return-form` |
| **収入登録** | 事業収入・その他の収入を登録 | `/income` |
| **経費登録** | 事業経費を登録・分類 | `/expense` |
| **損益一覧** | 登録済みの収入・経費を一覧表示 | `/profit-loss` |
| **損益レポート** | 損益計算結果をレポート表示 | `/report` |
| **設定** | ユーザー情報・税務設定の管理 | `/settings` |

---

## プロジェクト構成

```
tax-return/
├── pom.xml                          # Maven設定ファイル
├── README.md                        # このファイル
├── restart-tomcat.ps1               # Tomcat再起動スクリプト
│
├── src/main/
│   ├── java/org/example/
│   │   ├── Main.java                # エントリーポイント
│   │   ├── config/                  # Spring設定クラス
│   │   │   ├── WebConfig.java       # Webフレームワーク設定
│   │   │   └── WebAppInitializer.java
│   │   ├── controller/              # Webコントローラ層
│   │   │   └── HomeController.java  # ホーム・ナビゲーション制御
│   │   └── service/                 # ビジネスロジック層
│   │       ├── HomeService.java     # ホーム関連サービス
│   │       ├── impl/                # サービス実装クラス
│   │       ├── Auth/                # 認証関連
│   │       ├── Expense/             # 経費関連
│   │       ├── Income/              # 収入関連
│   │       ├── ProfitLoss/          # 損益関連
│   │       ├── Report/              # レポート関連
│   │       ├── ReturnForm/          # 申告書関連
│   │       └── Setting/             # 設定関連
│   │
│   ├── resources/
│   │   ├── application.properties    # アプリケーション設定
│   │   ├── css/                      # スタイルシート
│   │   │   ├── style.css             # 共通CSS
│   │   │   ├── expense/              # 経費画面用CSS
│   │   │   ├── footer/               # フッター用CSS
│   │   │   ├── header/               # ヘッダー用CSS
│   │   │   ├── home/                 # ホーム用CSS
│   │   │   ├── income/               # 収入画面用CSS
│   │   │   ├── profit-loss/          # 損益画面用CSS
│   │   │   ├── report/               # レポート用CSS
│   │   │   ├── return-form/          # 申告書用CSS
│   │   │   └── settings/             # 設定用CSS
│   │   └── js/
│   │       └── app.js                # JavaScript（共通）
│   │
│   └── webapp/WEB-INF/
│       └── views/                    # JSPテンプレート
│           ├── header.jsp            # ヘッダーコンポーネント
│           ├── footer.jsp            # フッターコンポーネント
│           ├── home.jsp              # ホーム画面
│           ├── return-form.jsp       # 申告書画面
│           ├── income.jsp            # 収入登録画面
│           ├── expense.jsp           # 経費登録画面
│           ├── profit-loss.jsp       # 損益一覧画面
│           ├── report.jsp            # レポート画面
│           └── settings.jsp          # 設定画面
│
└── target/                           # ビルド出力ディレクトリ
    ├── final-tax-return-1.0-SNAPSHOT.war
    └── classes/                      # コンパイル済みクラス
```

---

## アーキテクチャ

### 3層構造

```
┌─────────────────────────────────────────┐
│       プレゼンテーション層               │
│  JSP/HTML/CSS/JavaScript                │
│  (WEB-INF/views/)                       │
└─────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────┐
│         コントローラ層 (Controller)       │
│  Spring MVC @Controller                 │
│  - リクエスト処理                       │
│  - ビジネスロジックの呼び出し           │
│  - ビューへのデータ渡し                 │
└─────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────┐
│      ビジネスロジック層 (Service)        │
│  Spring @Service                        │
│  - 業務ロジック実装                     │
│  - データ処理・計算                     │
│  - リポジトリの呼び出し                 │
└─────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────┐
│    永続化層 (Repository/JPA)            │
│  Spring Data JPA @Repository            │
│  - データベース操作（CRUD）              │
│  - トランザクション管理                 │
└─────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────┐
│      データベース層 (MySQL)               │
│  AWS RDS for MySQL                      │
└─────────────────────────────────────────┘
```

---

## セットアップ・実行手順

### 前提条件
- Java 23 以上
- Maven 3.6 以上
- MySQL 8.0 以上
- Tomcat 11.0.21 以上

### ビルド
```bash
cd tax-return
mvn clean package
```

### デプロイ（Tomcat）
```bash
# WARファイルをTomcatのwebappsディレクトリにコピー
cp target/final-tax-return-1.0-SNAPSHOT.war $TOMCAT_HOME/webapps/

# Tomcatを起動
$TOMCAT_HOME/bin/startup.sh   # Linux/Mac
# または
$TOMCAT_HOME/bin/startup.bat  # Windows
```

### アクセス
```
http://localhost:8080/final-tax-return-1.0-SNAPSHOT/
```

---

## データベース設定

`application.properties` でデータベース接続情報を設定：

```properties
spring.datasource.url=jdbc:mysql://[HOST]:[PORT]/[DATABASE]
spring.datasource.username=[USER]
spring.datasource.password=[PASSWORD]
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update  # 自動スキーマ生成
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

---

## 開発メモ

### Spring設定
- **@EnableWebMvc**: Spring MVCの有効化
- **@ComponentScan**: 指定パッケージのコンポーネント自動検出
- **ViewResolver**: JSP テンプレートの解決設定
  - Prefix: `/WEB-INF/views/`
  - Suffix: `.jsp`

### 静的リソース
- CSS/JavaScript: `/resources/` 配下
- 各画面ごとにCSS: `resources/css/[screen-name]/`

---

## ライセンス
- 開発中

## 参考資料
- Spring MVC: https://spring.io/projects/spring-framework
- Hibernate JPA: https://hibernate.org/orm/
- Tomcat: https://tomcat.apache.org/
