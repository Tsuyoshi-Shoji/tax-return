# コーディングガイドライン

## 1. 基本方針

* 可読性・保守性を最優先とする
* 過度な抽象化を避ける
* シンプルで追いやすい実装を行う
* 既存のプロジェクト構成・命名規則に従う

---

## 2. アーキテクチャ

本プロジェクトは以下の3層構造を採用する。

Controller → Service → Repository

### Controller

責務:

* リクエスト受付
* バリデーション
* Service呼び出し
* View返却

禁止事項:

* 業務ロジック実装
* Repository直接呼び出し
* 複雑な計算処理

### Service

責務:

* 業務ロジック
* 金額計算
* トランザクション管理

### Repository

責務:

* DBアクセスのみ

禁止事項:

* 業務ロジック実装

---

## 3. DI（Dependency Injection）

コンストラクタインジェクションを使用する。

推奨:

```java id="f8gq8o"
private final ExpenseService expenseService;

public ExpenseController(
        ExpenseService expenseService) {
    this.expenseService = expenseService;
}
```

禁止:

```java id="j2b70f"
@Autowired
private ExpenseService expenseService;
```

---

## 4. 金額計算

金額計算では必ず BigDecimal を使用する。

禁止:

* double
* float

推奨:

```java id="7uz4b6"
BigDecimal total =
    income.subtract(expense);
```

丸め処理は明示すること。

```java id="jlwmxa"
value.setScale(2, RoundingMode.HALF_UP);
```

---

## 5. JSPルール

JSPでは scriptlet を使用しない。

禁止:

```jsp id="2wj7lq"
<% %>
```

JSTL と EL式を使用すること。

---

## 6. Transaction

@Transactional は Service層で使用する。

ControllerでTransaction管理を行わない。

---

## 7. DTO / Form

Entityを画面へ直接渡さない。

画面入力には Form クラスを使用する。

例:

* ExpenseForm
* IncomeForm

---

## 8. ログ出力

System.out.println は使用しない。

SLF4J を使用する。

---

## 9. 禁止事項

以下は禁止とする。

* React導入
* Thymeleaf導入
* field injection
* utilityクラス乱立
* Controllerへの業務ロジック実装
* 金額計算でdouble使用
