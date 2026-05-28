# 画面設計書 - ホーム画面

[← インデックスへ戻る](index.md)

---

## 画面概要

ログイン後のトップ画面。収益・支出のクイックアクセス、最近の取引履歴、収支サマリー（当年度・当月の全データ版・事業収支版）、確定申告期限カウントダウン、その他機能へのナビゲーションを表示する。

- **JSP ファイル**: `Home/home.jsp`
- **URL パス**: `/`
- **HTTP メソッド**: GET

---

## 画面項目一覧

| 項目名 | 種別 | モデル属性 / 説明 |
|--------|------|-----------------|
| クイックアクセス - 収益登録ボタン | リンクボタン | `/income` へ遷移 |
| クイックアクセス - 支出登録ボタン | リンクボタン | `/expense` へ遷移 |
| 最近の取引テーブル | 一覧表示 | `recentTransactions`（日付・区分・項目・金額） |
| 取引なし案内リンク | リンク | `recentTransactions` が空の場合に表示。`/income` へ遷移 |
| 全データ収支サマリー（当年度）収入 | 表示 | `annualAllProfitLoss.income` |
| 全データ収支サマリー（当年度）支出 | 表示 | `annualAllProfitLoss.expense` |
| 全データ収支サマリー（当年度）利益 | 表示 | `annualAllProfitLoss.profit` |
| 全データ収支サマリー（当月）収入 | 表示 | `monthlyAllProfitLoss.income` |
| 全データ収支サマリー（当月）支出 | 表示 | `monthlyAllProfitLoss.expense` |
| 全データ収支サマリー（当月）利益 | 表示 | `monthlyAllProfitLoss.profit` |
| 事業収支サマリー（当年度）収入 | 表示 | `annualBusinessProfitLoss.income` |
| 事業収支サマリー（当年度）支出 | 表示 | `annualBusinessProfitLoss.expense` |
| 事業収支サマリー（当年度）利益 | 表示 | `annualBusinessProfitLoss.profit` |
| 事業収支サマリー（当月）収入 | 表示 | `monthlyBusinessProfitLoss.income` |
| 事業収支サマリー（当月）支出 | 表示 | `monthlyBusinessProfitLoss.expense` |
| 事業収支サマリー（当月）利益 | 表示 | `monthlyBusinessProfitLoss.profit` |
| 確定申告期限カウントダウン（残り日数） | 表示 | `daysUntilDeadline` |
| 申告期限近接警告 | 表示（条件付き） | `isApproachingDeadline` が `true` の場合に警告文を表示 |
| 損益一覧リンク | リンク | `/profit-loss` へ遷移 |
| 損益レポートリンク | リンク | `/report` へ遷移 |
| 申告書作成リンク | リンク | `/return-form` へ遷移 |
| 設定リンク | リンク | `/settings` へ遷移 |

---

## イベント一覧

| イベント | 操作 | 処理内容 |
|----------|------|---------|
| クイックアクセス - 収益登録クリック | クリック | `GET /income` へ遷移 |
| クイックアクセス - 支出登録クリック | クリック | `GET /expense` へ遷移 |
| 損益一覧リンククリック | クリック | `GET /profit-loss` へ遷移 |
| 損益レポートリンククリック | クリック | `GET /report` へ遷移 |
| 申告書作成リンククリック | クリック | `GET /return-form` へ遷移 |
| 設定リンククリック | クリック | `GET /settings` へ遷移 |
| 取引なし案内リンククリック | クリック | `GET /income` へ遷移 |

---

## バリデーションチェック（フロント）仕様

なし（表示専用画面）

---

## 関連クラス

| 種別 | クラス名 | パッケージ |
|------|----------|-----------|
| Controller | `HomeController` | `org.example.controller` |
| Service | `HomeService` | `org.example.features.Home.service` |
| Service 実装 | `HomeServiceImpl` | `org.example.features.Home.service.impl` |
| Entity | `Income` | `org.example.entity` |
| Entity | `Expense` | `org.example.entity` |

---

*作成日: 2026-05-26*

