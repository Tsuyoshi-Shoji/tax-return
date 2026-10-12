<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="ja_JP" scope="page" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ホーム | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261009a">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261009a">
</head>
<body class="home-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main" tabindex="-1">
        <div class="home-container">
            <c:set var="hasHomeData" value="${not empty homeView.summary and not empty homeView.selectedFiscalYearId}" />

            <section class="welcome-section" aria-labelledby="welcome-message">
                <div class="welcome-copy">
                    <h1 id="welcome-message">おかえりなさい、<c:out value="${currentUserDisplayName}" default="ユーザー" />さん</h1>
                    <p id="welcome-description">確定申告の準備状況や資産のサマリーを確認できます。</p>
                </div>
                <form class="fiscal-year-form" method="get" action="${pageContext.request.contextPath}/home">
                    <label for="select-fiscal-year">対象年度</label>
                    <select id="select-fiscal-year" name="fiscalYearId" <c:if test="${empty homeView.fiscalYearOptions}">disabled</c:if>>
                        <c:if test="${empty homeView.fiscalYearOptions}"><option>年度データなし</option></c:if>
                        <c:forEach var="fy" items="${homeView.fiscalYearOptions}">
                            <option value="${fy.id}" <c:if test="${fy.id eq homeView.selectedFiscalYearId}">selected</c:if>><c:out value="${fy.label}" /></option>
                        </c:forEach>
                    </select>
                    <button class="year-apply" type="submit" <c:if test="${empty homeView.fiscalYearOptions}">disabled</c:if>>表示</button>
                </form>
            </section>

            <c:if test="${not hasHomeData and empty failureMessage}">
                <p class="home-notice" role="status">対象年度が登録されていません。集計データはまだ表示できません。</p>
            </c:if>

            <section class="summary-grid" aria-label="収支サマリー">
                <article class="home-card metric-card assets-card">
                    <span class="metric-icon tone-green"><span class="ui-icon icon-coins" aria-hidden="true"></span></span>
                    <div class="metric-copy">
                        <h2>総資産額</h2>
                        <%-- totalAssets currently contains annual profit, not an asset balance. --%>
                        <p id="display-total-assets" class="metric-value" aria-label="総資産額は未連携">— <span class="unit">円</span></p>
                        <p class="metric-note">資産残高は未連携です</p>
                        <p class="metric-explanation">収支の差額は、総損益で確認できます。</p>
                    </div>
                </article>
                <article class="home-card metric-card profit-card">
                    <span class="metric-icon tone-purple"><span class="ui-icon icon-profit" aria-hidden="true"></span></span>
                    <div class="metric-copy">
                        <h2>当年度の総損益</h2>
                        <p id="display-annual-profit-loss" class="metric-value"><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.annualProfitLoss}" pattern="#,##0" /></c:when><c:otherwise>—</c:otherwise></c:choose> <span class="unit">円</span></p>
                        <p id="display-prev-month-diff" class="metric-note">月次損益の前月比<br><span class="comparison-value ${homeView.summary.prevMonthDiff lt 0 ? 'is-negative' : (homeView.summary.prevMonthDiff gt 0 ? 'is-positive' : '')}"><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.prevMonthDiff}" pattern="+#,##0;-#,##0" /> 円</c:when><c:otherwise>—</c:otherwise></c:choose></span></p>
                    </div>
                </article>
                <article class="home-card flow-card">
                    <div class="flow-heading"><span class="metric-icon tone-green"><span class="ui-icon icon-trend" aria-hidden="true"></span></span><h2>収益</h2></div>
                    <dl class="flow-values">
                        <div><dt>当年度の収益</dt><dd id="display-annual-income"><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.annualIncome}" pattern="#,##0" /></c:when><c:otherwise>—</c:otherwise></c:choose> <span class="unit">円</span></dd></div>
                        <div><dt>今月の収益</dt><dd id="display-monthly-income"><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.monthlyIncome}" pattern="#,##0" /></c:when><c:otherwise>—</c:otherwise></c:choose> <span class="unit">円</span></dd></div>
                    </dl>
                    <a class="card-action card-action-link" href="${pageContext.request.contextPath}/income">収益を登録<span class="ui-icon icon-chevron-right" aria-hidden="true"></span></a>
                </article>
                <article class="home-card flow-card">
                    <div class="flow-heading"><span class="metric-icon tone-red"><span class="ui-icon icon-trend" aria-hidden="true"></span></span><h2>支出</h2></div>
                    <dl class="flow-values">
                        <div><dt>当年度の支出</dt><dd id="display-annual-expense"><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.annualExpense}" pattern="#,##0" /></c:when><c:otherwise>—</c:otherwise></c:choose> <span class="unit">円</span></dd></div>
                        <div><dt>今月の支出</dt><dd id="display-monthly-expense"><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.monthlyExpense}" pattern="#,##0" /></c:when><c:otherwise>—</c:otherwise></c:choose> <span class="unit">円</span></dd></div>
                    </dl>
                    <a class="card-action card-action-link" href="${pageContext.request.contextPath}/expense">支出を登録<span class="ui-icon icon-chevron-right" aria-hidden="true"></span></a>
                </article>
            </section>

            <div class="analysis-grid">
                <section class="home-card breakdown-card" aria-labelledby="breakdown-title">
                    <div class="card-heading"><h2 id="breakdown-title">当月の収益・支出内訳</h2><span id="link-view-details" class="unavailable">詳細は準備中</span></div>
                    <c:set var="monthlyTotal" value="${homeView.summary.monthlyIncome + homeView.summary.monthlyExpense}" />
                    <c:set var="canChart" value="${hasHomeData and monthlyTotal gt 0 and homeView.summary.monthlyIncome ge 0 and homeView.summary.monthlyExpense ge 0}" />
                    <%-- Keep ratio precision independent of the monetary values' decimal scale. --%>
                    <c:set var="incomeShare" value="${canChart ? homeView.summary.monthlyIncome.divide(monthlyTotal, 6, 'HALF_UP') * 100 : 0}" />
                    <%-- A fixed locale keeps the CSS percentage valid for every request locale. --%>
                    <fmt:setLocale value="en_US" scope="page" />
                    <fmt:formatNumber var="chartShare" value="${incomeShare}" pattern="0.####" groupingUsed="false" />
                    <fmt:setLocale value="ja_JP" scope="page" />
                    <div class="breakdown-content">
                        <div id="chart-monthly-breakdown" class="donut-chart ${canChart ? 'has-data' : ''}" style="--income-share: ${chartShare}%;" role="img" aria-labelledby="breakdown-title chart-description" aria-describedby="table-breakdown-items">
                            <div class="donut-center">
                                <c:choose>
                                    <c:when test="${canChart}">
                                        <span>今月の収支合計</span>
                                        <strong><fmt:formatNumber value="${monthlyTotal}" pattern="#,##0" /></strong>
                                        <span>円</span><small>収益 ＋ 支出</small>
                                    </c:when>
                                    <c:otherwise><span class="ui-icon icon-bars" aria-hidden="true"></span><strong class="chart-empty-label">内訳なし</strong><small>金額は右の一覧に表示</small></c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div class="breakdown-legend">
                            <table id="table-breakdown-items" class="breakdown-table">
                                <caption class="visually-hidden">当月の収益・支出の金額と構成比</caption>
                                <thead class="visually-hidden"><tr><th scope="col">項目</th><th scope="col">金額</th><th scope="col">構成比</th></tr></thead>
                                <tbody>
                                    <tr><th scope="row"><span class="legend-dot income-dot" aria-hidden="true"></span>収益（合計）</th><td><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.monthlyIncome}" pattern="#,##0" /> 円</c:when><c:otherwise>—</c:otherwise></c:choose></td><td><c:choose><c:when test="${canChart}"><fmt:formatNumber value="${incomeShare}" pattern="0.0" />%</c:when><c:otherwise>—</c:otherwise></c:choose></td></tr>
                                    <tr><th scope="row"><span class="legend-dot expense-dot" aria-hidden="true"></span>支出（合計）</th><td><c:choose><c:when test="${hasHomeData}"><fmt:formatNumber value="${homeView.summary.monthlyExpense}" pattern="#,##0" /> 円</c:when><c:otherwise>—</c:otherwise></c:choose></td><td><c:choose><c:when test="${canChart}"><fmt:formatNumber value="${100 - incomeShare}" pattern="0.0" />%</c:when><c:otherwise>—</c:otherwise></c:choose></td></tr>
                                </tbody>
                            </table>
                            <p id="chart-description" class="chart-note"><c:choose><c:when test="${canChart}">収益と支出の合計に対する構成比です。</c:when><c:when test="${not hasHomeData}">集計データがありません。</c:when><c:when test="${homeView.summary.monthlyIncome lt 0 or homeView.summary.monthlyExpense lt 0}">負の金額を含むため円グラフは表示できません。</c:when><c:otherwise>当月の収益・支出はありません。</c:otherwise></c:choose><br>項目別の内訳は未連携です。</p>
                            <p class="chart-note">当月は現在の月と選択年度が重なる期間です。</p>
                        </div>
                    </div>
                </section>

                <section class="home-card deadline-section" aria-labelledby="deadline-title">
                    <div class="card-heading"><h2 id="deadline-title">確定申告まで</h2></div>
                    <div class="deadline-content">
                        <img class="deadline-icon" src="${pageContext.request.contextPath}/css/icons/calendar.svg" width="100" height="112" alt="">
                        <div class="deadline-copy">
                            <p id="display-days-until-deadline" class="countdown">
                                <c:choose>
                                    <c:when test="${hasHomeData and not empty homeView.deadlineDate and homeView.daysUntilDeadline gt 0}"><span>あと</span><strong><c:out value="${homeView.daysUntilDeadline}" /></strong><span>日</span></c:when>
                                    <c:when test="${hasHomeData and not empty homeView.deadlineDate and homeView.daysUntilDeadline eq 0}"><strong class="deadline-state">本日</strong></c:when>
                                    <c:when test="${hasHomeData and not empty homeView.deadlineDate}"><strong class="deadline-state">期限経過</strong></c:when>
                                    <c:otherwise><strong class="deadline-state">未設定</strong></c:otherwise>
                                </c:choose>
                            </p>
                            <p id="display-deadline-date">提出期限の目安：<c:choose><c:when test="${hasHomeData and not empty homeView.deadlineDate}"><c:out value="${homeView.deadlineDate}" /></c:when><c:otherwise>—</c:otherwise></c:choose></p>
                        </div>
                    </div>
                    <div id="message-deadline-note" class="deadline-note">
                        <span class="ui-icon icon-info" aria-hidden="true"></span>
                        <p><c:choose><c:when test="${hasHomeData and homeView.daysUntilDeadline ge 0 and homeView.daysUntilDeadline le 30}">申告期限の目安が近づいています。</c:when><c:otherwise>必要な書類や取引の入力漏れを確認しましょう。</c:otherwise></c:choose><br><span>対象年度・休日等による正式な期限は公式案内をご確認ください。</span></p>
                    </div>
                </section>
            </div>

            <div class="activity-grid">
                <section class="home-card information-card" aria-labelledby="information-title">
                    <div class="card-heading"><h2 id="information-title">情報</h2><span id="link-view-all-info" class="unavailable">一覧は準備中</span></div>
                    <table id="table-information" class="information-table">
                        <caption class="visually-hidden">お知らせ情報</caption>
                        <thead class="visually-hidden"><tr><th scope="col">日付</th><th scope="col">タイプ</th><th scope="col">内容</th></tr></thead>
                        <tbody><tr><td colspan="3"><div class="empty-state"><span class="empty-icon ui-icon icon-bell" aria-hidden="true"></span><strong>表示できるお知らせはありません</strong><p>メモ・相談・通知の連携は準備中です。</p></div></td></tr></tbody>
                    </table>
                </section>
                <section class="home-card recent-transactions" aria-labelledby="transactions-title">
                    <div class="card-heading"><h2 id="transactions-title">最近の取引</h2><span id="link-view-all-transactions" class="unavailable">取引一覧は準備中</span></div>
                    <div class="table-scroll" role="region" aria-label="最近の取引一覧（横スクロール可能）" tabindex="0">
                        <table id="table-recent-transactions" class="transactions-table">
                            <caption class="visually-hidden">選択年度の最近の取引、最大5件</caption>
                            <thead><tr><th scope="col">日付</th><th scope="col">内容</th><th scope="col" class="amount-column">金額</th></tr></thead>
                            <tbody>
                                <c:forEach var="transaction" items="${homeView.recentTransactions}">
                                    <tr>
                                        <td class="date-cell"><c:out value="${transaction.date}" /></td>
                                        <td class="description-cell"><span class="transaction-details" title="<c:out value="${transaction.type}" /> · <c:out value="${transaction.category}" />"><c:out value="${transaction.details}" default="詳細なし" /></span><span class="visually-hidden">区分：<c:out value="${transaction.type}" />、項目：<c:out value="${transaction.category}" /></span></td>
                                        <td class="amount-column ${transaction.type eq '収益' ? 'income-amount' : (transaction.type eq '支出' ? 'expense-amount' : '')}"><span class="visually-hidden"><c:out value="${transaction.type}" /> </span><c:if test="${transaction.amount gt 0}"><c:choose><c:when test="${transaction.type eq '収益'}">+</c:when><c:when test="${transaction.type eq '支出'}">−</c:when></c:choose></c:if><fmt:formatNumber value="${transaction.amount}" pattern="#,##0" />円</td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty homeView.recentTransactions}">
                                    <tr><td colspan="3"><div class="empty-state"><span class="empty-icon ui-icon icon-document" aria-hidden="true"></span><strong><c:choose><c:when test="${hasHomeData}">この年度の取引はありません</c:when><c:otherwise>取引データを表示できません</c:otherwise></c:choose></strong><p>登録済みの取引がここに表示されます。</p></div></td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
<jsp:include page="/WEB-INF/views/Common/app-dialog.jsp" />
</body>
</html>
