<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>損益詳細レポート</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/report/report.css" />
</head>
<body>
    <jsp:include page="/WEB-INF/views/Header/header.jsp" />

    <main class="report-main">
        <div class="report-container report-detail-container">
            <div class="page-header">
                <h1>損益詳細レポート</h1>
                <p class="page-subtitle"><span id="detailYearMonth"></span> の損益内訳を確認できます。</p>
            </div>

            <section class="report-section" aria-labelledby="allDetailTitle" data-detail-section="all">
                <div class="section-heading">
                    <h2 id="allDetailTitle">全データ詳細レポート</h2>
                    <a class="close-btn" href="${pageContext.request.contextPath}/report?year=${reportDetailViewAll.year}">年度レポートへ戻る</a>
                </div>
                <table class="report-table detail-month-table">
                    <thead>
                        <tr>
                            <th>区分</th>
                            <th>金額</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr class="income-row"><th>収益</th><td id="allDetailIncome">¥0</td></tr>
                        <tr class="expense-row"><th>支出</th><td id="allDetailExpense">¥0</td></tr>
                        <tr class="profit-row"><th>損益</th><td id="allDetailProfit">¥0</td></tr>
                    </tbody>
                </table>

                <section class="report-section nested-report-section">
                    <div class="detail-layout">
                        <div class="detail-table-container">
                            <h2>収益 項目別累計</h2>
                            <table class="detail-table">
                                <thead><tr><th>項目</th><th>金額</th></tr></thead>
                                <tbody id="allIncomeBreakdownBody"></tbody>
                            </table>
                        </div>
                        <div class="chart-pie-container">
                            <h2>収益割合</h2>
                            <div class="pie-chart empty-pie" id="allIncomePie"><span>画面右表の構成比を確認してください</span></div>
                        </div>
                    </div>

                    <div class="detail-layout">
                        <div class="detail-table-container">
                            <h2>支出 項目別累計</h2>
                            <table class="detail-table">
                                <thead><tr><th>項目</th><th>金額</th></tr></thead>
                                <tbody id="allExpenseBreakdownBody"></tbody>
                            </table>
                        </div>
                        <div class="chart-pie-container">
                            <h2>支出割合</h2>
                            <div class="pie-chart empty-pie" id="allExpensePie"><span>画面右表の構成比を確認してください</span></div>
                        </div>
                    </div>
                </section>

                <section class="report-section nested-report-section">
                    <div class="section-heading">
                        <h2>発生日カレンダー</h2>
                        <span>青ドット: 収益 / 赤ドット: 支出</span>
                    </div>
                    <div class="calendar" id="allCalendar"></div>
                </section>
            </section>

            <section class="report-section" aria-labelledby="businessDetailTitle" data-detail-section="business">
                <div class="section-heading">
                    <h2 id="businessDetailTitle">事業収支詳細レポート</h2>
                    <a class="close-btn" href="${pageContext.request.contextPath}/report?year=${reportDetailViewBusiness.year}">年度レポートへ戻る</a>
                </div>
                <table class="report-table detail-month-table">
                    <thead>
                        <tr>
                            <th>区分</th>
                            <th>金額</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr class="income-row"><th>収益</th><td id="businessDetailIncome">¥0</td></tr>
                        <tr class="expense-row"><th>支出</th><td id="businessDetailExpense">¥0</td></tr>
                        <tr class="profit-row"><th>損益</th><td id="businessDetailProfit">¥0</td></tr>
                    </tbody>
                </table>

                <section class="report-section nested-report-section">
                    <div class="detail-layout">
                        <div class="detail-table-container">
                            <h2>収益 項目別累計</h2>
                            <table class="detail-table">
                                <thead><tr><th>項目</th><th>金額</th></tr></thead>
                                <tbody id="businessIncomeBreakdownBody"></tbody>
                            </table>
                        </div>
                        <div class="chart-pie-container">
                            <h2>収益割合</h2>
                            <div class="pie-chart empty-pie" id="businessIncomePie"><span>画面右表の構成比を確認してください</span></div>
                        </div>
                    </div>

                    <div class="detail-layout">
                        <div class="detail-table-container">
                            <h2>支出 項目別累計</h2>
                            <table class="detail-table">
                                <thead><tr><th>項目</th><th>金額</th></tr></thead>
                                <tbody id="businessExpenseBreakdownBody"></tbody>
                            </table>
                        </div>
                        <div class="chart-pie-container">
                            <h2>支出割合</h2>
                            <div class="pie-chart empty-pie" id="businessExpensePie"><span>画面右表の構成比を確認してください</span></div>
                        </div>
                    </div>
                </section>

                <section class="report-section nested-report-section">
                    <div class="section-heading">
                        <h2>発生日カレンダー</h2>
                        <span>青ドット: 収益 / 赤ドット: 支出</span>
                    </div>
                    <div class="calendar" id="businessCalendar"></div>
                </section>
            </section>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/Footer/footer.jsp" />

    <script>
        (function () {
            'use strict';

            const detailData = {
                all: {
                    year: Number('${reportDetailViewAll.year}'),
                    month: Number('${reportDetailViewAll.month}'),
                    incomeBreakdown: [
                        <c:forEach var="row" items="${reportDetailViewAll.incomeBreakdown}" varStatus="status">
                        { item: '${row.item}', amount: Number('${row.amount}') }<c:if test="${not status.last}">,</c:if>
                        </c:forEach>
                    ],
                    expenseBreakdown: [
                        <c:forEach var="row" items="${reportDetailViewAll.expenseBreakdown}" varStatus="status">
                        { item: '${row.item}', amount: Number('${row.amount}') }<c:if test="${not status.last}">,</c:if>
                        </c:forEach>
                    ],
                    calendarEvents: [
                        <c:forEach var="event" items="${reportDetailViewAll.calendarEvents}" varStatus="status">
                        { day: ${event.day}, type: '${event.type}' }<c:if test="${not status.last}">,</c:if>
                        </c:forEach>
                    ]
                },
                business: {
                    year: Number('${reportDetailViewBusiness.year}'),
                    month: Number('${reportDetailViewBusiness.month}'),
                    incomeBreakdown: [
                        <c:forEach var="row" items="${reportDetailViewBusiness.incomeBreakdown}" varStatus="status">
                        { item: '${row.item}', amount: Number('${row.amount}') }<c:if test="${not status.last}">,</c:if>
                        </c:forEach>
                    ],
                    expenseBreakdown: [
                        <c:forEach var="row" items="${reportDetailViewBusiness.expenseBreakdown}" varStatus="status">
                        { item: '${row.item}', amount: Number('${row.amount}') }<c:if test="${not status.last}">,</c:if>
                        </c:forEach>
                    ],
                    calendarEvents: [
                        <c:forEach var="event" items="${reportDetailViewBusiness.calendarEvents}" varStatus="status">
                        { day: ${event.day}, type: '${event.type}' }<c:if test="${not status.last}">,</c:if>
                        </c:forEach>
                    ]
                }
            };

            function formatCurrency(value) {
                return '¥' + Number(value || 0).toLocaleString('ja-JP');
            }

            function sum(rows) {
                return rows.reduce((total, row) => total + Number(row.amount || 0), 0);
            }

            function renderBreakdown(section, bodyId, rows) {
                const body = section.querySelector('#' + bodyId);
                body.innerHTML = '';
                if (rows.length === 0) {
                    const tr = document.createElement('tr');
                    tr.innerHTML = '<td colspan="2" class="empty-row">データなし</td>';
                    body.appendChild(tr);
                    return;
                }
                rows.forEach(row => {
                    const tr = document.createElement('tr');
                    const item = document.createElement('td');
                    const amount = document.createElement('td');
                    item.textContent = row.item;
                    amount.textContent = formatCurrency(row.amount);
                    tr.appendChild(item);
                    tr.appendChild(amount);
                    body.appendChild(tr);
                });
            }

            function renderCalendar(section, calendarId, events, year, month) {
                const calendar = section.querySelector('#' + calendarId);
                const firstDay = new Date(year, month - 1, 1);
                const lastDate = new Date(year, month, 0).getDate();
                const startOffset = firstDay.getDay();
                const weekLabels = ['日', '月', '火', '水', '木', '金', '土'];

                calendar.innerHTML = '';
                weekLabels.forEach(label => {
                    const cell = document.createElement('div');
                    cell.className = 'calendar-head';
                    cell.textContent = label;
                    calendar.appendChild(cell);
                });
                for (let i = 0; i < startOffset; i++) {
                    const blank = document.createElement('div');
                    blank.className = 'calendar-cell is-blank';
                    calendar.appendChild(blank);
                }
                for (let day = 1; day <= lastDate; day++) {
                    const cell = document.createElement('div');
                    cell.className = 'calendar-cell';
                    const dayLabel = document.createElement('span');
                    dayLabel.textContent = String(day);
                    const dots = document.createElement('span');
                    dots.className = 'calendar-dots';
                    if (events.some(event => event.day === day && event.type === 'income')) {
                        const dot = document.createElement('span');
                        dot.className = 'dot income-dot';
                        dots.appendChild(dot);
                    }
                    if (events.some(event => event.day === day && event.type === 'expense')) {
                        const dot = document.createElement('span');
                        dot.className = 'dot expense-dot';
                        dots.appendChild(dot);
                    }
                    cell.appendChild(dayLabel);
                    cell.appendChild(dots);
                    calendar.appendChild(cell);
                }
            }

            function renderPieChart(section, elementId, rows, colors) {
                const target = section.querySelector('#' + elementId);
                if (!target) {
                    return;
                }
                if (!rows.length) {
                    target.classList.add('empty-pie');
                    target.style.background = 'var(--color-bg)';
                    return;
                }
                const total = sum(rows);
                let start = 0;
                const segments = rows.map((row, index) => {
                    const ratio = total === 0 ? 0 : (Number(row.amount) / total) * 100;
                    const end = start + ratio;
                    const color = colors[index % colors.length];
                    const segment = color + ' ' + start + '% ' + end + '%';
                    start = end;
                    return segment;
                });
                target.classList.remove('empty-pie');
                target.style.background = 'conic-gradient(' + segments.join(', ') + ')';
                target.innerHTML = '<span>' + rows.length + '項目</span>';
            }

            function renderDetail(sectionId, key) {
                const section = document.querySelector('[data-detail-section="' + sectionId + '"]');
                const data = detailData[key];
                const totalIncome = sum(data.incomeBreakdown);
                const totalExpense = sum(data.expenseBreakdown);
                const profit = totalIncome - totalExpense;

                document.getElementById('detailYearMonth').textContent = data.year + '年' + data.month + '月';
                section.querySelector('#' + key + 'DetailIncome').textContent = formatCurrency(totalIncome);
                section.querySelector('#' + key + 'DetailExpense').textContent = formatCurrency(totalExpense);
                section.querySelector('#' + key + 'DetailProfit').textContent = formatCurrency(profit);
                renderBreakdown(section, key + 'IncomeBreakdownBody', data.incomeBreakdown);
                renderBreakdown(section, key + 'ExpenseBreakdownBody', data.expenseBreakdown);
                renderCalendar(section, key + 'Calendar', data.calendarEvents, data.year, data.month);
                renderPieChart(section, key + 'IncomePie', data.incomeBreakdown, ['#2563eb', '#3b82f6', '#60a5fa', '#93c5fd', '#1d4ed8']);
                renderPieChart(section, key + 'ExpensePie', data.expenseBreakdown, ['#dc2626', '#ef4444', '#f87171', '#fca5a5', '#991b1b']);
            }

            renderDetail('all', 'all');
            renderDetail('business', 'business');
        })();
    </script>
</body>
</html>
