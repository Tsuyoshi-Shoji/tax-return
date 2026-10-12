<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="ja_JP" scope="page" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>レポート詳細 | マネタックス</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/home.css?v=20261011c">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/profit-loss/profit-loss.css?v=20261011d">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/report/report.css?v=20261011c">
    <script src="${pageContext.request.contextPath}/css/report/report.js?v=20261011c" defer></script>
</head>
<body class="home-page profit-loss-page report-page">
<jsp:include page="/WEB-INF/views/Header/header.jsp" />
<div class="home-shell">
    <jsp:include page="/WEB-INF/views/Common/sidebar.jsp" />
    <main id="main-content" class="home-main profit-loss-main" tabindex="-1">
        <div class="profit-loss-container">
            <nav class="profit-loss-breadcrumb" aria-label="パンくず">
                <ol>
                    <li><a href="${pageContext.request.contextPath}/home">ホーム</a></li>
                    <li><a id="link-back-year-report" href="${pageContext.request.contextPath}/report?year=${page.year}">レポート</a></li>
                    <li aria-current="page"><c:out value="${page.monthLabel}" /></li>
                </ol>
            </nav>

            <div class="report-title-row">
                <section class="profit-loss-title-area" aria-labelledby="page-title">
                    <h1 id="page-title">レポート詳細</h1>
                    <p id="page-description">選択した月の収支・損益の詳細を確認できます。各項目の内訳や発生した取引をカレンダーで確認できます。</p>
                </section>
                <form id="month-form" class="report-controls" method="get" action="${pageContext.request.contextPath}/report/detail">
                    <input type="hidden" name="year" value="${page.year}" />
                    <c:choose>
                        <c:when test="${page.hasPrev}"><a id="button-prev-month" class="month-nav" href="${pageContext.request.contextPath}/report/detail?year=${page.prevYear}&amp;month=${page.prevMonth}" aria-label="前月">&lsaquo;</a></c:when>
                        <c:otherwise><span id="button-prev-month" class="month-nav is-disabled" aria-disabled="true">&lsaquo;</span></c:otherwise>
                    </c:choose>
                    <label id="label-month-select" class="visually-hidden" for="month-select">選択月</label>
                    <select id="month-select" name="month">
                        <c:forEach var="opt" items="${page.monthOptions}">
                            <option value="${opt.month}" <c:if test="${opt.month == page.month}">selected</c:if>><c:out value="${opt.label}" /></option>
                        </c:forEach>
                    </select>
                    <c:choose>
                        <c:when test="${page.hasNext}"><a id="button-next-month" class="month-nav" href="${pageContext.request.contextPath}/report/detail?year=${page.nextYear}&amp;month=${page.nextMonth}" aria-label="次月">&rsaquo;</a></c:when>
                        <c:otherwise><span id="button-next-month" class="month-nav is-disabled" aria-disabled="true">&rsaquo;</span></c:otherwise>
                    </c:choose>
                    <noscript><button type="submit">表示</button></noscript>
                </form>
            </div>

            <c:set var="labelIncome" value="選択月の累計収益" />
            <c:set var="labelExpense" value="選択月の累計支出" />
            <c:set var="labelProfit" value="選択月の損益" />

            <section class="profit-loss-card report-section" aria-labelledby="heading-all-data-detail-report">
                <h2 id="heading-all-data-detail-report" class="report-section-title">全データ月次レポート（申告対象外を含む）</h2>
                <p class="report-section-desc">事業収支、配当金、雑収入など、すべての収支データを含めた選択月のレポートです。</p>
                <c:set var="section" value="${page.allData}" />
                <c:set var="idIncome" value="display-monthly-income-all" />
                <c:set var="idExpense" value="display-monthly-expense-all" />
                <c:set var="idProfit" value="display-monthly-profit-all" />
                <%@ include file="/WEB-INF/views/Report/_summary-cards.jspf" %>
                <div class="report-charts report-charts-4">
                    <c:set var="idIncomeHeading" value="heading-income-breakdown-all-detail" />
                    <c:set var="idIncomeTable" value="table-income-breakdown-all-detail" />
                    <c:set var="idIncomePie" value="chart-income-pie-all-detail" />
                    <c:set var="idExpenseHeading" value="heading-expense-breakdown-all-detail" />
                    <c:set var="idExpenseTable" value="table-expense-breakdown-all-detail" />
                    <c:set var="idExpensePie" value="chart-expense-pie-all-detail" />
                    <%@ include file="/WEB-INF/views/Report/_breakdown.jspf" %>
                </div>
            </section>

            <section class="profit-loss-card report-section" aria-labelledby="heading-business-detail-report">
                <h2 id="heading-business-detail-report" class="report-section-title">事業収支月次レポート（申告対象のみ）</h2>
                <p class="report-section-desc">確定申告の対象となる事業収支データのみを集計した選択月のレポートです。</p>
                <c:set var="section" value="${page.business}" />
                <c:set var="idIncome" value="display-monthly-income-business" />
                <c:set var="idExpense" value="display-monthly-expense-business" />
                <c:set var="idProfit" value="display-monthly-profit-business" />
                <%@ include file="/WEB-INF/views/Report/_summary-cards.jspf" %>
                <div class="report-charts report-charts-4">
                    <c:set var="idIncomeHeading" value="heading-income-breakdown-business-detail" />
                    <c:set var="idIncomeTable" value="table-income-breakdown-business-detail" />
                    <c:set var="idIncomePie" value="chart-income-pie-business-detail" />
                    <c:set var="idExpenseHeading" value="heading-expense-breakdown-business-detail" />
                    <c:set var="idExpenseTable" value="table-expense-breakdown-business-detail" />
                    <c:set var="idExpensePie" value="chart-expense-pie-business-detail" />
                    <%@ include file="/WEB-INF/views/Report/_breakdown.jspf" %>
                </div>
            </section>

            <section class="profit-loss-card report-section" aria-labelledby="heading-transaction-calendar">
                <div class="calendar-head">
                    <div>
                        <h2 id="heading-transaction-calendar" class="report-section-title">発生カレンダー</h2>
                        <p class="report-section-desc">選択した月に発生した収益・支出・申告対象外の取引をカレンダーで確認できます。</p>
                    </div>
                    <span class="chart-legend"><i class="dot dot-income"></i>収益 <i class="dot dot-expense"></i>支出 <i class="dot dot-excluded"></i>申告対象外</span>
                </div>
                <table id="calendar-display" class="calendar-table">
                    <thead>
                    <tr>
                        <th scope="col" class="sun">日</th><th scope="col">月</th><th scope="col">火</th><th scope="col">水</th>
                        <th scope="col">木</th><th scope="col">金</th><th scope="col" class="sat">土</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="week" items="${page.calendarWeeks}">
                        <tr>
                            <c:forEach var="day" items="${week}" varStatus="st">
                                <td class="${day.inMonth ? '' : 'is-outside'} ${st.index == 0 ? 'sun' : (st.index == 6 ? 'sat' : '')}">
                                    <span class="cal-day">${day.day}</span>
                                    <span class="cal-dots">
                                        <c:if test="${day.hasIncome}"><i class="dot dot-income" title="収益"></i></c:if>
                                        <c:if test="${day.hasExpense}"><i class="dot dot-expense" title="支出"></i></c:if>
                                        <c:if test="${day.hasExcluded}"><i class="dot dot-excluded" title="申告対象外"></i></c:if>
                                    </span>
                                </td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </section>
        </div>
    </main>
</div>
<jsp:include page="/WEB-INF/views/Footer/footer.jsp" />
</body>
</html>