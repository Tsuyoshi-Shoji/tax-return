<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ホーム - 確定申告支援システム</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css" />
</head>
<body>
    <!-- ヘッダーをインクルード -->
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <!-- メインコンテンツ -->
    <main>
        <div class="home-container">
            <!-- クイックアクセスセクション -->
            <section class="quick-access">
                <h2>クイックアクセス</h2>
                <div class="quick-access-grid">
                    <a href="${pageContext.request.contextPath}/income" class="quick-btn income-btn">
                        <div class="btn-icon">💰</div>
                        <div class="btn-text">
                            <h3>収益登録</h3>
                            <p>収入を記録</p>
                        </div>
                    </a>
                    <a href="${pageContext.request.contextPath}/expense" class="quick-btn expense-btn">
                        <div class="btn-icon">💸</div>
                        <div class="btn-text">
                            <h3>支出登録</h3>
                            <p>経費を記録</p>
                        </div>
                    </a>
                </div>
            </section>

            <!-- 最近の取引履歴セクション -->
            <section class="recent-transactions">
                <h2>最近の取引</h2>
                <c:if test="${not empty recentTransactions}">
                    <table class="transactions-table">
                        <thead>
                            <tr>
                                <th>日付</th>
                                <th>区分</th>
                                <th>項目</th>
                                <th class="amount-column">金額</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="transaction" items="${recentTransactions}">
                                <tr class="transaction-row ${transaction.type == '収益' ? 'income-row' : 'expense-row'}">
                                    <td class="date-cell">${transaction.date}</td>
                                    <td class="type-cell">
                                        <span class="badge ${transaction.type == '収益' ? 'badge-income' : 'badge-expense'}">
                                            ${transaction.type}
                                        </span>
                                    </td>
                                    <td class="category-cell">${transaction.category}</td>
                                    <td class="amount-cell">
                                        <span class="amount-value">¥<fmt:formatNumber value="${transaction.amount}" pattern="#,##0" /></span>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:if>
                <c:if test="${empty recentTransactions}">
                    <div class="no-data">
                        <p>取引がまだ登録されていません</p>
                        <a href="${pageContext.request.contextPath}/income" class="link-btn">最初の取引を登録する</a>
                    </div>
                </c:if>
            </section>

            <!-- ダッシュボードセクション -->
            <section class="dashboard">
                <h2>収支サマリー</h2>
                <div class="dashboard-grid">
                    <div class="dashboard-card annual-card">
                        <h3>全データ収支サマリー</h3>
                        <div class="profit-loss-summary">
                            <div class="summary-period">
                                <h4>当年度</h4>
                                <div class="summary-item income-item">
                                    <span class="label">収入</span>
                                    <span class="value income-value">¥<fmt:formatNumber value="${annualAllProfitLoss.income}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-item expense-item">
                                    <span class="label">支出</span>
                                    <span class="value expense-value">¥<fmt:formatNumber value="${annualAllProfitLoss.expense}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-divider"></div>
                                <div class="summary-item profit-item">
                                    <span class="label">利益</span>
                                    <span class="value profit-value">¥<fmt:formatNumber value="${annualAllProfitLoss.profit}" pattern="#,##0" /></span>
                                </div>
                            </div>
                            <div class="summary-period">
                                <h4>当月</h4>
                                <div class="summary-item income-item">
                                    <span class="label">収入</span>
                                    <span class="value income-value">¥<fmt:formatNumber value="${monthlyAllProfitLoss.income}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-item expense-item">
                                    <span class="label">支出</span>
                                    <span class="value expense-value">¥<fmt:formatNumber value="${monthlyAllProfitLoss.expense}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-divider"></div>
                                <div class="summary-item profit-item">
                                    <span class="label">利益</span>
                                    <span class="value profit-value">¥<fmt:formatNumber value="${monthlyAllProfitLoss.profit}" pattern="#,##0" /></span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="dashboard-card monthly-card">
                        <h3>事業収支サマリー</h3>
                        <div class="profit-loss-summary">
                            <div class="summary-period">
                                <h4>当年度</h4>
                                <div class="summary-item income-item">
                                    <span class="label">収入</span>
                                    <span class="value income-value">¥<fmt:formatNumber value="${annualBusinessProfitLoss.income}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-item expense-item">
                                    <span class="label">支出</span>
                                    <span class="value expense-value">¥<fmt:formatNumber value="${annualBusinessProfitLoss.expense}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-divider"></div>
                                <div class="summary-item profit-item">
                                    <span class="label">利益</span>
                                    <span class="value profit-value">¥<fmt:formatNumber value="${annualBusinessProfitLoss.profit}" pattern="#,##0" /></span>
                                </div>
                            </div>
                            <div class="summary-period">
                                <h4>当月</h4>
                                <div class="summary-item income-item">
                                    <span class="label">収入</span>
                                    <span class="value income-value">¥<fmt:formatNumber value="${monthlyBusinessProfitLoss.income}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-item expense-item">
                                    <span class="label">支出</span>
                                    <span class="value expense-value">¥<fmt:formatNumber value="${monthlyBusinessProfitLoss.expense}" pattern="#,##0" /></span>
                                </div>
                                <div class="summary-divider"></div>
                                <div class="summary-item profit-item">
                                    <span class="label">利益</span>
                                    <span class="value profit-value">¥<fmt:formatNumber value="${monthlyBusinessProfitLoss.profit}" pattern="#,##0" /></span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <!-- 申告期限カウントダウンセクション -->
            <section class="deadline-section">
                <div class="deadline-card ${isApproachingDeadline ? 'deadline-warning' : 'deadline-normal'}">
                    <h3>確定申告期限</h3>
                    <div class="deadline-content">
                        <div class="countdown-days ${isApproachingDeadline ? 'countdown-alert' : ''}">
                            <span class="days-number">${daysUntilDeadline}</span>
                            <span class="days-label">日</span>
                        </div>
                        <div class="deadline-info">
                            <p>次の申告期限まで残り日数</p>
                            <p class="deadline-date">3月15日（翌営業日）</p>
                            <c:if test="${isApproachingDeadline}">
                                <p class="deadline-warning-text">⚠ 申告期限が近づいています。早めの準備をお勧めします。</p>
                            </c:if>
                        </div>
                    </div>
                </div>
            </section>

            <!-- その他のアクションセクション -->
            <section class="other-actions">
                <h2>その他のアクション</h2>
                <div class="action-links">
                    <a href="${pageContext.request.contextPath}/profit-loss" class="action-link">
                        <span class="action-icon">📊</span>
                        <span class="action-text">損益一覧</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/report" class="action-link">
                        <span class="action-icon">📈</span>
                        <span class="action-text">損益レポート</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/return-form" class="action-link">
                        <span class="action-icon">📋</span>
                        <span class="action-text">申告書作成</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/settings" class="action-link">
                        <span class="action-icon">⚙️</span>
                        <span class="action-text">設定</span>
                    </a>
                </div>
            </section>
        </div>
    </main>

    <!-- フッターをインクルード -->
    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>


