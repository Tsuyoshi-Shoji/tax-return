# 画面設計書 インデックス

## 対象プロジェクト

確定申告支援システム（個人事業主向け財務管理 Web アプリケーション）

---

## 画面一覧

| No | 画面名 | JSP ファイル | URL パス | 設計書                                                                                                |
|----|--------|-------------|---------|----------------------------------------------------------------------------------------------------|
| 1 | ログイン | `Auth/login.jsp` | `/login` | [screen-00-login_spec.md](00_auth/screen-00-login_spec.md)                                         |
| 2 | ユーザー新規登録 | `Auth/user-register.jsp` | `/register` | [screen-01-user-register_spec.md](01_user-register/screen-01-user-register_spec.md)                |
| 3 | ホーム | `Home/home.jsp` | `/` | [screen-02-home_spec.md](02_home/screen-02-home_spec.md)                                           |
| 4 | 確定申告 | `ReturnForm/return-form.jsp` | `/return-form` | [screen-03-tax-return_spec.md](03_taxt-return/screen-03-tax-return_spec.md)                        |
| 5 | 収益登録 | `Income/income.jsp` | `/income` | [screen-04-income-register_spec.md](04_income-register/screen-04-income-register_spec.md)          |
| 6 | 支出登録 | `Expense/expense.jsp` | `/expense` | [screen-05-expense-register_spec.md](05_expense-register/screen-05-expense-register_spec.md)       |
| 7 | 損益検索・一覧 | `ProfitLoss/profit-loss.jsp` | `/profit-loss` | [screen-06-profit-loss_spec.md](06_profit-loss-list/screen-06-profit-loss_spec.md)                 |
| 8 | 損益詳細 | `ProfitLoss/profit-loss-detail.jsp` | `/profit-loss/detail` | [screen-07-profit-loss-detail_spec.md](07_profit-loss-detail/screen-07-profit-loss-detail_spec.md) |
| 9 | 損益レポート | `Report/report.jsp` | `/report` | [screen-08-report_spec.md](08_report/screen-08-report_spec.md)                                     |
| 10 | 損益詳細レポート | `Report/report-detail.jsp` | `/report/detail` | [screen-09-report-detail_spec.md](09_report-detail/screen-09-report-detail_spec.md)                |
| 11 | 設定 | `Setting/settings.jsp` | `/settings` | [screen-10-settings_spec.md](10_settings/screen-10-settings_spec.md)                               |
| 12 | AIアシスタント | `Assistant/chat-window.jsx` | フローティングウィジェット | [screen-11-assistant-ai_spec.md](11_assistant-ai/screen-11-assistant-ai_spec.md)                   |

> 共通部品: `Header/header.jsp`、`Footer/footer.jsp`（全画面に `jsp:include` で組み込み）  
> グローバルエラーハンドリング: `GlobalControllerAdvice`（`org.example.controller`）  
> AIアシスタント: 常時右下にフローティングアイコン表示、クリックでチャットウィンドウを表示

---

*作成日: 2026-05-26*  
*更新日: 2026-10-08*
