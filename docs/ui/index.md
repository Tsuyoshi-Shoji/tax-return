# 画面設計書 インデックス

## 対象プロジェクト

確定申告支援システム（個人事業主向け財務管理 Web アプリケーション）

---

## 画面一覧

| No | 画面名 | JSP ファイル | URL パス | 設計書 |
|----|--------|-------------|---------|--------|
| 1 | ホーム | `Home/home.jsp` | `/` | [screen-01-home.md](screen-01-home.md) |
| 2 | 収益登録 | `Income/income.jsp` | `/income` | [screen-02-income.md](screen-02-income.md) |
| 3 | 支出登録 | `Expense/expense.jsp` | `/expense` | [screen-03-expense.md](screen-03-expense.md) |
| 4 | 損益検索・一覧 | `ProfitLoss/profit-loss.jsp` | `/profit-loss` | [screen-04-profit-loss.md](screen-04-profit-loss.md) |
| 5 | 損益詳細 | `ProfitLoss/profit-loss-detail.jsp` | `/profit-loss/detail` | [screen-05-profit-loss-detail.md](screen-05-profit-loss-detail.md) |
| 6 | 損益レポート | `Report/report.jsp` | `/report` | [screen-06-report.md](screen-06-report.md) |
| 7 | 損益詳細レポート | `Report/report-detail.jsp` | `/report/detail` | [screen-07-report-detail.md](screen-07-report-detail.md) |
| 8 | 申告書作成 | `ReturnForm/return-form.jsp` | `/return-form` | [screen-08-return-form.md](screen-08-return-form.md) |
| 9 | 設定 | `Setting/settings.jsp` | `/settings` | [screen-09-settings.md](screen-09-settings.md) |
| 10 | ログイン | `Auth/login.jsp` | `/login` | [screen-10-login.md](screen-10-login.md) |
| 11 | ユーザー新規登録 | `Auth/user-register.jsp` | `/register` | [screen-11-user-register.md](screen-11-user-register.md) |

> 共通部品: `Header/header.jsp`、`Footer/footer.jsp`（全画面に `jsp:include` で組み込み）  
> グローバルエラーハンドリング: `GlobalControllerAdvice`（`org.example.controller`）

---

*作成日: 2026-05-26*

