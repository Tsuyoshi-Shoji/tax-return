# 命名規則

このドキュメントは、tax-returnプロジェクトにおける統一的な命名規則を定義しています。

---

## Java 命名規則

### クラス・インターフェース

| 種類 | ルール | 例 |
|------|--------|------|
| **Java Class** | PascalCase | `HomeController`, `UserService`, `ExpenseEntity` |
| **Java Method** | camelCase | `getUserById()`, `calculateTotalExpense()`, `validateForm()` |
| **Package** | lowercase (ドット区切り) | `org.example.controller`, `org.example.service.impl` |
| **Constant** | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| **Variable** | camelCase | `userName`, `expenseTotal`, `isActive` |

### アーキテクチャレイヤー別命名

| コンポーネント | ルール | 例 |
|-------------|--------|------|
| **Entity** | PascalCase (単数形) | `User`, `Expense`, `Income`（✓ OK）<br/>❌ `Users`, `Expenses` は不可 |
| **Repository** | `XxxRepository` | `UserRepository`, `ExpenseRepository`, `IncomeRepository` |
| **Service** | `XxxService` | `UserService`, `ExpenseService`, `IncomeService` |
| **ServiceImpl** | `XxxServiceImpl` | `UserServiceImpl`, `ExpenseServiceImpl`, `IncomeServiceImpl` |
| **Controller** | `XxxController` | `UserController`, `ExpenseController`, `IncomeController` |
| **Form/Request** | `XxxForm` | `UserForm`, `ExpenseForm`, `IncomeForm` |
| **DTO (Data Transfer Object)** | `XxxDto` | `UserDto`, `ExpenseDto`, `IncomeDto` |

---

## Web 命名規則

### URL (REST エンドポイント)

| パターン | ルール | 例 |
|---------|--------|------|
| **URL Path** | kebab-case (ハイフン区切り) | `/api/user-profiles`, `/return-form`, `/profit-loss` |
| **Query Parameter** | camelCase | `?userId=123`, `?startDate=2024-01-01` |

### HTML / CSS

| 対象 | ルール | 例 |
|------|--------|------|
| **CSS Class** | kebab-case | `user-profile-card`, `expense-table`, `form-input` |
| **CSS ID** | kebab-case | `main-container`, `header-nav`, `footer-section` |
| **JavaScript Variable** | camelCase | `userName`, `expenseTotal`, `isFormValid` |

---

## データベース命名規則

### テーブル・カラム

| 対象 | ルール | 例 |
|------|--------|------|
| **Table Name** | snake_case (複数形推奨) | `users`, `expenses`, `incomes`, `return_forms` |
| **Column Name** | snake_case (単数形) | `user_id`, `expense_amount`, `created_at`, `is_active` |
| **Primary Key** | `id` または `xxx_id` | `id`, `user_id` |
| **Foreign Key** | `xxx_id` | `user_id`, `expense_category_id` |
| **Boolean Column** | `is_xxx` | `is_active`, `is_deleted`, `is_paid` |
| **Timestamp** | `created_at`, `updated_at` | `created_at`, `updated_at` |

### 例：テーブル定義

```sql
-- テーブル定義例
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE expenses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    expense_category_id INT,
    amount DECIMAL(10, 2) NOT NULL,
    description TEXT,
    expense_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (expense_category_id) REFERENCES expense_categories(id)
);
```

---

## ファイル・ディレクトリ命名規則

### Java ファイル

| 対象 | ルール | 例 |
|------|--------|------|
| **クラスファイル** | PascalCase + `.java` | `HomeController.java`, `UserService.java` |
| **パッケージディレクトリ** | lowercase | `controller/`, `service/`, `entity/` |

### JSP テンプレート

| 対象 | ルール | 例 |
|------|--------|------|
| **JSP ファイル名** | kebab-case + `.jsp` | `home.jsp`, `return-form.jsp`, `profit-loss.jsp` |
| **ディレクトリ名** | PascalCase | `Home/`, `Expense/`, `Income/`, `ProfitLoss/` |

### CSS / JavaScript

| 対象 | ルール | 例 |
|------|--------|------|
| **CSS ファイル名** | kebab-case + `.css` | `style.css`, `form-input.css`, `header-nav.css` |
| **JavaScript ファイル名** | kebab-case + `.js` | `app.js`, `form-validator.js`, `api-client.js` |

---

## プロジェクト構造

```
tax-return/
├── src/main/java/org/example/          # ← lowercase package
│   ├── config/                         # ← lowercase directory
│   │   ├── WebConfig.java             # ← PascalCase class
│   │   └── WebAppInitializer.java
│   ├── controller/                     # ← lowercase directory
│   │   ├── HomeController.java         # ← XxxController
│   │   └── ExpenseController.java
│   ├── service/                        # ← lowercase directory
│   │   ├── HomeService.java            # ← XxxService (interface)
│   │   ├── impl/
│   │   │   └── HomeServiceImpl.java    # ← XxxServiceImpl
│   │   └── ExpenseService.java
│   ├── entity/                         # ← lowercase directory
│   │   ├── User.java                   # ← PascalCase (singular)
│   │   └── Expense.java
│   ├── repository/                     # ← lowercase directory
│   │   ├── UserRepository.java         # ← XxxRepository
│   │   └── ExpenseRepository.java
│   └── form/                           # ← lowercase directory
│       ├── UserForm.java               # ← XxxForm
│       └── ExpenseForm.java
├── src/main/resources/
│   ├── css/                            # ← lowercase directory
│   │   ├── style.css                  # ← kebab-case file
│   │   ├── expense/                   # ← lowercase directory
│   │   │   └── expense.css
│   │   └── income/
│   │       └── income.css
│   └── js/
│       └── app.js                      # ← kebab-case file
└── src/main/webapp/WEB-INF/views/
    ├── Home/                           # ← PascalCase directory
    │   └── home.jsp                    # ← kebab-case file
    ├── Expense/
    │   └── expense.jsp
    ├── Income/
    │   └── income.jsp
    └── ProfitLoss/
        └── profit-loss.jsp
```

---

## 命名規則チェックリスト

実装時に以下を確認してください：

- [ ] クラス名は PascalCase になっているか
- [ ] メソッド名は camelCase になっているか
- [ ] パッケージ名は lowercase になっているか
- [ ] Entity クラスは単数形か
- [ ] Repository は `XxxRepository` の形式か
- [ ] Service は `XxxService` の形式か
- [ ] ServiceImpl は `XxxServiceImpl` の形式か
- [ ] Controller は `XxxController` の形式か
- [ ] Form は `XxxForm` の形式か
- [ ] DTO は `XxxDto` の形式か
- [ ] URL/Path は kebab-case か
- [ ] CSS class/id は kebab-case か
- [ ] JavaScript 変数名は camelCase か
- [ ] DB テーブル名は snake_case（複数形）か
- [ ] DB カラム名は snake_case（単数形）か
- [ ] JSP ファイル名は kebab-case か
- [ ] CSS/JS ファイル名は kebab-case か

---

## 参考資料

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Oracle Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-136091.html)
- [REST API Best Practices](https://restfulapi.net/)
